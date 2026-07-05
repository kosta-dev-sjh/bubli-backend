package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WbsItemPublicServiceImplTest {

	@Mock
	WbsItemRepository wbsItemRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	WbsItemPublicServiceImpl wbsItemPublicService;

	@Test
	void createRetriesWhenAutoOrderConflicts() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		given(wbsItemRepository.findMaxOrderNo(roomId, null)).willReturn(1, 2);
		given(wbsItemRepository.saveAndFlush(any(WbsItem.class)))
				.willThrow(new DataIntegrityViolationException("duplicate wbs order"))
				.willAnswer(invocation -> {
					WbsItem item = invocation.getArgument(0);
					ReflectionTestUtils.setField(item, "id", itemId);
					return item;
				});

		WbsItemResult result = wbsItemPublicService.create(userId, roomId, new CreateWbsItemCommand(
				null,
				"에이전트 WBS",
				null,
				WbsStatus.TODO
		));

		assertThat(result.id()).isEqualTo(itemId);
		assertThat(result.orderNo()).isEqualTo(3);
		verify(wbsItemRepository, times(2)).saveAndFlush(any(WbsItem.class));
	}

	@Test
	void createRejectsDuplicatedExplicitOrder() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(wbsItemRepository.existsSiblingOrder(roomId, null, 1)).willReturn(true);

		assertThatThrownBy(() -> wbsItemPublicService.create(userId, roomId, new CreateWbsItemCommand(
				null,
				"중복 순서",
				1,
				WbsStatus.TODO
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_400_002));
		verify(wbsItemRepository, never()).saveAndFlush(any(WbsItem.class));
	}
}
