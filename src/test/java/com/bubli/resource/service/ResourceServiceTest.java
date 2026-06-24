package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.RoomAccessService;
import com.bubli.resource.dto.CreateResourceCommand;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
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
