package com.bubli.resource.service;

import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.repository.ResourceStorageDeleteRequestRepository;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import com.bubli.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceStorageDeleteRetryWorker {

	static final String DEAD_LETTER_MESSAGE = "자료 저장소 객체 삭제 재시도 한도를 초과했습니다.";
	private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
	private static final List<ResourceStorageDeleteStatus> RETRYABLE_STATUSES = List.of(
			ResourceStorageDeleteStatus.PENDING,
			ResourceStorageDeleteStatus.FAILED
	);

	private final ResourceStorageDeleteRequestRepository resourceStorageDeleteRequestRepository;
	private final StorageService storageService;

	@Transactional
	public int retryDeleteRequests(int batchSize, int maxRetryCount) {
		if (batchSize <= 0) {
			return 0;
		}
		int deletedCount = 0;
		for (ResourceStorageDeleteRequest request : resourceStorageDeleteRequestRepository
				.findByStatusIn(
						RETRYABLE_STATUSES,
						PageRequest.of(0, batchSize, Sort.by("createdAt").ascending())
				)
				.getContent()) {
			if (request.getRetryCount() >= maxRetryCount) {
				request.markDeadLetter(DEAD_LETTER_MESSAGE);
				continue;
			}
			if (retryDelete(request, maxRetryCount)) {
				deletedCount++;
			}
		}
		return deletedCount;
	}

	private boolean retryDelete(ResourceStorageDeleteRequest request, int maxRetryCount) {
		try {
			storageService.delete(request.getStorageKey());
			request.markDeleted();
			return true;
		} catch (RuntimeException exception) {
			request.markFailed(errorMessage(exception));
			if (request.getRetryCount() >= maxRetryCount) {
				request.markDeadLetter(DEAD_LETTER_MESSAGE);
			}
			return false;
		}
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (!StringUtils.hasText(message)) {
			message = exception.getClass().getSimpleName();
		}
		if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}
}
