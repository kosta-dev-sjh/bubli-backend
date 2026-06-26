package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.dto.CreateProjectRoomCommand;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.dto.UpdateProjectRoomCommand;
import com.bubli.project.dto.UpdateProjectRoomPaymentCommand;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ProjectRoomServiceTest {

	@Mock
	ProjectRoomRepository projectRoomRepository;

	@Mock
	RoomMemberRepository roomMemberRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	ProjectRoomService projectRoomService;

	@Test
	void createProjectRoomAlsoCreatesLeaderMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		CreateProjectRoomCommand command = new CreateProjectRoomCommand(
				"브랜드 사이트 제작",
				"블루클라이언트",
				BigDecimal.valueOf(1_500_000),
				PaymentStatus.PENDING,
				LocalDate.parse("2026-07-10"),
				null,
				ProjectRoomStatus.ACTIVE
		);
		given(projectRoomRepository.save(any(ProjectRoom.class))).willAnswer(invocation -> {
			ProjectRoom projectRoom = invocation.getArgument(0);
			ReflectionTestUtils.setField(projectRoom, "id", roomId);
			return projectRoom;
		});

		ProjectRoomResult result = projectRoomService.create(userId, command);

		assertThat(result.id()).isEqualTo(roomId);
		assertThat(result.createdByUserId()).isEqualTo(userId);
		assertThat(result.name()).isEqualTo("브랜드 사이트 제작");

		ArgumentCaptor<RoomMember> memberCaptor = ArgumentCaptor.forClass(RoomMember.class);
		verify(roomMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getRoomId()).isEqualTo(roomId);
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(userId);
	}

	@Test
	void getProjectRoomRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		givenProjectAccessDenied(userId, roomId);

		assertThatThrownBy(() -> projectRoomService.getProjectRoom(userId, roomId))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void getProjectRoomReturnsRoomForActiveMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ProjectRoom projectRoom = ProjectRoom.create(
				userId,
				"앱 UI 개선",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);

		given(projectRoomRepository.findById(roomId)).willReturn(Optional.of(projectRoom));

		ProjectRoomResult result = projectRoomService.getProjectRoom(userId, roomId);

		assertThat(result.id()).isEqualTo(roomId);
		assertThat(result.name()).isEqualTo("앱 UI 개선");
	}

	@Test
	void updateProjectRoomRequiresProjectLeader() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		RoomMember member = RoomMember.createMember(roomId, userId);
		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(member));

		assertThatThrownBy(() -> projectRoomService.updateProjectRoom(
				userId,
				roomId,
				new UpdateProjectRoomCommand("이름 변경", null, null)
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void updateProjectRoomChangesBasicInfo() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		RoomMember leader = RoomMember.create(roomId, userId, RoomMemberRole.PROJECT_LEADER);
		ProjectRoom projectRoom = ProjectRoom.create(
				userId,
				"기존 프로젝트",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(projectRoomRepository.findById(roomId)).willReturn(Optional.of(projectRoom));

		ProjectRoomResult result = projectRoomService.updateProjectRoom(
				userId,
				roomId,
				new UpdateProjectRoomCommand("새 프로젝트", "새 클라이언트", ProjectRoomStatus.ACTIVE)
		);

		assertThat(result.name()).isEqualTo("새 프로젝트");
		assertThat(result.clientName()).isEqualTo("새 클라이언트");
		assertThat(result.status()).isEqualTo(ProjectRoomStatus.ACTIVE);
		assertThat(projectRoom.getName()).isEqualTo("새 프로젝트");
	}

	@Test
	void updateProjectRoomPaymentChangesPaymentFields() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		RoomMember leader = RoomMember.createLeader(roomId, userId);
		ProjectRoom projectRoom = ProjectRoom.create(
				userId,
				"입금 관리 프로젝트",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(projectRoomRepository.findById(roomId)).willReturn(Optional.of(projectRoom));

		ProjectRoomResult result = projectRoomService.updateProjectRoomPayment(
				userId,
				roomId,
				new UpdateProjectRoomPaymentCommand(
						BigDecimal.valueOf(2_000_000),
						PaymentStatus.PAID,
						LocalDate.parse("2026-07-20"),
						LocalDate.parse("2026-07-18")
				)
		);

		assertThat(result.contractAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000_000));
		assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(result.paymentDueDate()).isEqualTo(LocalDate.parse("2026-07-20"));
		assertThat(result.paidAt()).isEqualTo(LocalDate.parse("2026-07-18"));
	}

	@Test
	void closeProjectRoomMarksRoomClosed() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		RoomMember leader = RoomMember.createLeader(roomId, userId);
		ProjectRoom projectRoom = ProjectRoom.create(
				userId,
				"종료할 프로젝트",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);

		given(roomMemberRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(Optional.of(leader));
		given(projectRoomRepository.findById(roomId)).willReturn(Optional.of(projectRoom));

		ProjectRoomResult result = projectRoomService.closeProjectRoom(userId, roomId);

		assertThat(result.status()).isEqualTo(ProjectRoomStatus.CLOSED);
		assertThat(result.closedAt()).isNotNull();
		assertThat(projectRoom.getStatus()).isEqualTo(ProjectRoomStatus.CLOSED);
	}

	private void givenProjectAccessDenied(UUID userId, UUID roomId) {
		org.mockito.BDDMockito.willThrow(new BusinessException(ErrorCode.PROJECT_403_001))
				.given(projectMembershipPublicService)
				.assertActiveMember(userId, roomId);
	}
}
