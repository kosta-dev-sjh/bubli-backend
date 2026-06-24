package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.RoomAccessService;
import com.bubli.resource.dto.CreateResourceCommand;
import com.bubli.resource.dto.CreateResourceVersionRequest;
import com.bubli.resource.dto.ResourceCommentResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceVersionResult;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceComment;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceVersion;
import com.bubli.resource.repository.ResourceCommentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceVersionRepository;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

	@Mock
	ResourceRepository resourceRepository;

	@Mock
	ResourceCommentRepository resourceCommentRepository;

	@Mock
	ResourceFileRepository resourceFileRepository;

	@Mock
	ResourceVersionRepository resourceVersionRepository;

	@Mock
	RoomAccessService roomAccessService;

	@InjectMocks
	ResourceService resourceService;

	@Test
	void createPersonalResourceSavesOwnerOnlyResource() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		given(resourceRepository.save(any(Resource.class))).willAnswer(invocation -> {
			Resource resource = invocation.getArgument(0);
			ReflectionTestUtils.setField(resource, "id", resourceId);
			return resource;
		});

		ResourceResult result = resourceService.create(userId, new CreateResourceCommand(
				"계약서 초안",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				null
		));

		assertThat(result.id()).isEqualTo(resourceId);
		assertThat(result.ownerId()).isEqualTo(userId);
		assertThat(result.roomId()).isNull();
		assertThat(result.visibility()).isEqualTo(ResourceVisibility.PERSONAL);
		assertThat(result.status()).isEqualTo(ResourceStatus.READY);

		ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
		verify(resourceRepository).save(resourceCaptor.capture());
		assertThat(resourceCaptor.getValue().getOwnerId()).isEqualTo(userId);
		assertThat(resourceCaptor.getValue().getRoomId()).isNull();
	}

	@Test
	void createRoomSharedResourceRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomAccessService.isActiveMember(userId, roomId)).willReturn(false);

		assertThatThrownBy(() -> resourceService.create(userId, new CreateResourceCommand(
				"회의록",
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				roomId
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_403_001));
	}

	@Test
	void createRoomSharedResourceRequiresRoomId() {
		UUID userId = UUID.randomUUID();

		assertThatThrownBy(() -> resourceService.create(userId, new CreateResourceCommand(
				"요구사항",
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				null
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_400_001));
	}

	@Test
	void getPersonalResourcesReturnsOnlyOwnerPersonalResources() {
		UUID userId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"개인 자료",
				ResourceKind.MEMO,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		PageRequest pageable = PageRequest.of(0, 20);
		given(resourceRepository.findByOwnerIdAndVisibilityAndDeletedAtIsNull(
				any(UUID.class),
				any(ResourceVisibility.class),
				any(Pageable.class)
		)).willReturn(new PageImpl<>(List.of(resource), pageable, 1));

		PageResponse<ResourceResult> result = resourceService.getPersonalResources(userId, "personal", pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().title()).isEqualTo("개인 자료");
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(resourceRepository).findByOwnerIdAndVisibilityAndDeletedAtIsNull(
				eq(userId),
				eq(ResourceVisibility.PERSONAL),
				pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
	}

	@Test
	void updateResourceChangesTitleAfterAccessCheck() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"이전 제목",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));

		ResourceResult result = resourceService.updateResource(userId, resourceId, "새 제목");

		assertThat(result.title()).isEqualTo("새 제목");
		assertThat(resource.getTitle()).isEqualTo("새 제목");
	}

	@Test
	void deleteResourceMarksResourceDeleted() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"삭제할 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));

		resourceService.deleteResource(userId, resourceId);

		assertThat(resource.getDeletedAt()).isNotNull();
		assertThat(resource.getStatus()).isEqualTo(ResourceStatus.DELETED);
	}

	@Test
	void createCommentRequiresReadableResourceAndStoresAuthor() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"댓글 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));
		given(resourceCommentRepository.save(any(ResourceComment.class))).willAnswer(invocation -> {
			ResourceComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", commentId);
			return comment;
		});

		ResourceCommentResult result = resourceService.createComment(userId, resourceId, null, "확인했습니다");

		assertThat(result.id()).isEqualTo(commentId);
		assertThat(result.resourceId()).isEqualTo(resourceId);
		assertThat(result.authorId()).isEqualTo(userId);
		assertThat(result.body()).isEqualTo("확인했습니다");
	}

	@Test
	void getResourceCommentsRequiresReadableResource() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"댓글 목록 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		ResourceComment comment = ResourceComment.create(resourceId, userId, null, "첫 댓글");
		PageRequest pageable = PageRequest.of(0, 20);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));
		given(resourceCommentRepository.findByResourceIdAndDeletedAtIsNull(
				eq(resourceId),
				any(Pageable.class)
		)).willReturn(new PageImpl<>(List.of(comment), pageable, 1));

		PageResponse<ResourceCommentResult> result = resourceService.getResourceComments(userId, resourceId, pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().body()).isEqualTo("첫 댓글");
	}

	@Test
	void updateCommentRejectsNonAuthor() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		Resource resource = Resource.create(
				ownerId,
				null,
				"댓글 수정 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		ResourceComment comment = ResourceComment.create(resourceId, ownerId, null, "원래 댓글");
		given(resourceCommentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));

		assertThatThrownBy(() -> resourceService.updateComment(otherUserId, commentId, "수정"))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_403_001));
	}

	@Test
	void deleteCommentMarksCommentDeleted() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"댓글 삭제 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		ResourceComment comment = ResourceComment.create(resourceId, userId, null, "삭제할 댓글");
		given(resourceCommentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));

		resourceService.deleteComment(userId, commentId);

		assertThat(comment.getDeletedAt()).isNotNull();
	}

	@Test
	void createVersionCreatesFileAndNextVersionNo() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID fileId = UUID.randomUUID();
		UUID versionId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"버전 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));
		given(resourceFileRepository.save(any(ResourceFile.class))).willAnswer(invocation -> {
			ResourceFile file = invocation.getArgument(0);
			ReflectionTestUtils.setField(file, "id", fileId);
			return file;
		});
		given(resourceVersionRepository.findMaxVersionNo(resourceId)).willReturn(2);
		given(resourceVersionRepository.save(any(ResourceVersion.class))).willAnswer(invocation -> {
			ResourceVersion version = invocation.getArgument(0);
			ReflectionTestUtils.setField(version, "id", versionId);
			return version;
		});

		ResourceVersionResult result = resourceService.createVersion(userId, resourceId, new CreateResourceVersionRequest(
				"resources/%s/v3.pdf".formatted(resourceId),
				"계약서-v3.pdf",
				"application/pdf",
				1024L,
				"checksum"
		));

		assertThat(result.id()).isEqualTo(versionId);
		assertThat(result.fileId()).isEqualTo(fileId);
		assertThat(result.versionNo()).isEqualTo(3);
		assertThat(result.createdBy()).isEqualTo(userId);
		assertThat(result.originalName()).isEqualTo("계약서-v3.pdf");
	}

	@Test
	void getResourceVersionsReturnsFileMetadata() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID fileId = UUID.randomUUID();
		Resource resource = Resource.create(
				userId,
				null,
				"버전 목록 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		ResourceFile file = ResourceFile.create(
				resourceId,
				"resources/%s/v1.pdf".formatted(resourceId),
				"계약서.pdf",
				"application/pdf",
				2048L,
				null
		);
		ReflectionTestUtils.setField(file, "id", fileId);
		ResourceVersion version = ResourceVersion.create(resourceId, 1, fileId, userId);
		PageRequest pageable = PageRequest.of(0, 20);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));
		given(resourceVersionRepository.findByResourceId(eq(resourceId), any(Pageable.class)))
				.willReturn(new PageImpl<>(List.of(version), pageable, 1));
		given(resourceFileRepository.findById(fileId)).willReturn(Optional.of(file));

		PageResponse<ResourceVersionResult> result = resourceService.getResourceVersions(userId, resourceId, pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().versionNo()).isEqualTo(1);
		assertThat(result.getItems().getFirst().originalName()).isEqualTo("계약서.pdf");
	}

	@Test
	void getResourceRejectsOtherUserPersonalResource() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = Resource.create(
				ownerId,
				null,
				"개인 자료",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));

		assertThatThrownBy(() -> resourceService.getResource(otherUserId, resourceId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_403_001));
	}

	@Test
	void getRoomResourceRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		Resource resource = Resource.create(
				UUID.randomUUID(),
				roomId,
				"프로젝트룸 자료",
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				ResourceStatus.READY
		);
		given(resourceRepository.findByIdAndDeletedAtIsNull(resourceId)).willReturn(Optional.of(resource));
		given(roomAccessService.isActiveMember(userId, roomId)).willReturn(false);

		assertThatThrownBy(() -> resourceService.getResource(userId, resourceId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_403_001));
	}
}
