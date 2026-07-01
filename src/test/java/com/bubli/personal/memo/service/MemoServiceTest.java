package com.bubli.personal.memo.service;

import com.bubli.global.error.BusinessException;
import com.bubli.personal.memo.dto.CreateMemoRequest;
import com.bubli.personal.memo.dto.UpdateMemoRequest;
import com.bubli.personal.memo.entity.Memo;
import com.bubli.personal.memo.repository.MemoRepository;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

	@Mock
	MemoRepository memoRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	MemoService memoService;

	@Test
	void createPersonalMemoStoresAuthorOnly() {
		UUID userId = UUID.randomUUID();
		UUID memoId = UUID.randomUUID();
		given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> {
			Memo memo = invocation.getArgument(0);
			ReflectionTestUtils.setField(memo, "id", memoId);
			return memo;
		});

		var result = memoService.createPersonalMemo(
				userId,
				new CreateMemoRequest("오늘 회의에서 확인할 것").toCommand()
		);

		assertThat(result.id()).isEqualTo(memoId);
		assertThat(result.authorUserId()).isEqualTo(userId);
		assertThat(result.roomId()).isNull();
		assertThat(result.status()).isEqualTo(MemoStatus.ACTIVE);
	}

	@Test
	void createRoomMemoRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(memoRepository.save(any(Memo.class))).willAnswer(invocation -> invocation.getArgument(0));

		memoService.createRoomMemo(userId, roomId, new CreateMemoRequest("프로젝트 메모").toCommand());

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		ArgumentCaptor<Memo> memoCaptor = ArgumentCaptor.forClass(Memo.class);
		verify(memoRepository).save(memoCaptor.capture());
		assertThat(memoCaptor.getValue().getRoomId()).isEqualTo(roomId);
		assertThat(memoCaptor.getValue().getAuthorUserId()).isEqualTo(userId);
	}

	@Test
	void getPersonalMemosReturnsOnlyActivePersonalQuery() {
		UUID userId = UUID.randomUUID();
		Memo memo = Memo.createPersonal(userId, "개인 메모");
		given(memoRepository.findByAuthorUserIdAndRoomIdIsNullAndStatus(
				userId,
				MemoStatus.ACTIVE,
				Pageable.unpaged()
		)).willReturn(new PageImpl<>(List.of(memo)));

		var result = memoService.getPersonalMemos(userId, Pageable.unpaged());

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().body()).isEqualTo("개인 메모");
	}

	@Test
	void updatePersonalMemoRejectsOtherAuthor() {
		UUID ownerId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		UUID memoId = UUID.randomUUID();
		Memo memo = Memo.createPersonal(ownerId, "원본");
		ReflectionTestUtils.setField(memo, "id", memoId);
		given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

		assertThatThrownBy(() -> memoService.updateMemo(
				otherId,
				memoId,
				new UpdateMemoRequest("수정").toCommand()
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void deleteMemoMarksDeleted() {
		UUID userId = UUID.randomUUID();
		UUID memoId = UUID.randomUUID();
		Memo memo = Memo.createPersonal(userId, "삭제할 메모");
		ReflectionTestUtils.setField(memo, "id", memoId);
		given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

		memoService.deleteMemo(userId, memoId);

		assertThat(memo.getStatus()).isEqualTo(MemoStatus.DELETED);
	}

	@Test
	void deletedMemoIsTreatedAsNotFound() {
		UUID userId = UUID.randomUUID();
		UUID memoId = UUID.randomUUID();
		Memo memo = Memo.createPersonal(userId, "이미 삭제됨");
		ReflectionTestUtils.setField(memo, "id", memoId);
		memo.delete();
		given(memoRepository.findById(memoId)).willReturn(Optional.of(memo));

		assertThatThrownBy(() -> memoService.deleteMemo(userId, memoId))
				.isInstanceOf(BusinessException.class);
	}
}
