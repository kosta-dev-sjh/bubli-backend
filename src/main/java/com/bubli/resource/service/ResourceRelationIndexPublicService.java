package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceRelation;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.bubli.resource.repository.ResourceRelationRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.type.ResourceVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceRelationIndexPublicService {

	private static final int MAX_RELATIONS = 5;
	private static final double MIN_SIMILARITY_SCORE = 0.70D;
	private static final String REASON = "SIMILAR_CONTENT";

	private final ResourceEmbeddingRepository resourceEmbeddingRepository;
	private final ResourceRelationRepository resourceRelationRepository;
	private final ResourceRepository resourceRepository;

	@Transactional
	public RelationIndexResult rebuildRelations(Resource resource) {
		List<ResourceEmbedding> sourceEmbeddings = resourceEmbeddingRepository
				.findAllByResourceIdOrderByChunkIndex(resource.getId());
		resourceRelationRepository.deleteByResourceIdOrRelatedResourceId(resource.getId(), resource.getId());

		if (sourceEmbeddings.isEmpty()) {
			return RelationIndexResult.indexed(0);
		}

		List<ResourceEmbedding> candidateEmbeddings = findCandidateEmbeddings(resource);
		if (candidateEmbeddings.isEmpty()) {
			return RelationIndexResult.indexed(0);
		}

		List<float[]> sourceVectors = sourceEmbeddings.stream()
				.map(ResourceEmbedding::getEmbedding)
				.map(this::parseVectorLiteral)
				.toList();
		Map<UUID, Double> bestScoresByResource = new HashMap<>();
		for (ResourceEmbedding candidateEmbedding : candidateEmbeddings) {
			if (resource.getId().equals(candidateEmbedding.getResourceId())) {
				continue;
			}
			if (!isExistingResource(candidateEmbedding.getResourceId())) {
				continue;
			}
			float[] candidateVector = parseVectorLiteral(candidateEmbedding.getEmbedding());
			double score = bestSimilarity(sourceVectors, candidateVector);
			bestScoresByResource.merge(candidateEmbedding.getResourceId(), score, Math::max);
		}

		List<ResourceRelation> relations = bestScoresByResource.entrySet().stream()
				.filter(entry -> entry.getValue() >= MIN_SIMILARITY_SCORE)
				.sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
				.limit(MAX_RELATIONS)
				.flatMap(entry -> List.of(
						ResourceRelation.create(resource.getId(), entry.getKey(), REASON, toScore(entry.getValue())),
						ResourceRelation.create(entry.getKey(), resource.getId(), REASON, toScore(entry.getValue()))
				).stream())
				.toList();
		resourceRelationRepository.saveAll(relations);
		return RelationIndexResult.indexed(relations.size());
	}

	private List<ResourceEmbedding> findCandidateEmbeddings(Resource resource) {
		if (resource.getVisibility() == ResourceVisibility.PERSONAL) {
			return resourceEmbeddingRepository.findAllByOwnerIdAndVisibility(
					resource.getOwnerId(),
					ResourceVisibility.PERSONAL
			);
		}
		if (resource.getVisibility() == ResourceVisibility.ROOM_SHARED) {
			return resourceEmbeddingRepository.findAllByRoomIdAndVisibility(
					resource.getRoomId(),
					ResourceVisibility.ROOM_SHARED
			);
		}
		return List.of();
	}

	private boolean isExistingResource(UUID resourceId) {
		Optional<Resource> resource = resourceRepository.findByIdAndDeletedAtIsNull(resourceId);
		return resource.isPresent();
	}

	private double bestSimilarity(List<float[]> sourceVectors, float[] candidateVector) {
		double bestScore = -1.0D;
		for (float[] sourceVector : sourceVectors) {
			bestScore = Math.max(bestScore, cosineSimilarity(sourceVector, candidateVector));
		}
		return bestScore;
	}

	private double cosineSimilarity(float[] left, float[] right) {
		if (left.length != right.length) {
			throw new IllegalArgumentException("Embedding dimensions must match.");
		}
		double dot = 0.0D;
		double leftNorm = 0.0D;
		double rightNorm = 0.0D;
		for (int index = 0; index < left.length; index++) {
			dot += left[index] * right[index];
			leftNorm += left[index] * left[index];
			rightNorm += right[index] * right[index];
		}
		if (leftNorm == 0.0D || rightNorm == 0.0D) {
			return 0.0D;
		}
		return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
	}

	private float[] parseVectorLiteral(String vectorLiteral) {
		if (vectorLiteral == null || vectorLiteral.length() < 2
				|| vectorLiteral.charAt(0) != '[' || vectorLiteral.charAt(vectorLiteral.length() - 1) != ']') {
			throw new IllegalArgumentException("Invalid vector literal.");
		}
		String body = vectorLiteral.substring(1, vectorLiteral.length() - 1);
		if (body.isBlank()) {
			return new float[0];
		}
		String[] parts = body.split(",");
		float[] vector = new float[parts.length];
		for (int index = 0; index < parts.length; index++) {
			vector[index] = Float.parseFloat(parts[index].trim());
		}
		return vector;
	}

	private BigDecimal toScore(double score) {
		return BigDecimal.valueOf(score).setScale(5, RoundingMode.HALF_UP);
	}

	public record RelationIndexResult(int relationCount) {

		static RelationIndexResult indexed(int relationCount) {
			return new RelationIndexResult(relationCount);
		}
	}
}
