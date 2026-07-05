package com.bubli.widget.repository;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemStateValue;
import com.bubli.widget.type.WidgetItemType;
import com.bubli.widget.type.WidgetMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class WidgetRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	WidgetContextSettingRepository contextSettingRepository;

	@Autowired
	WidgetBubbleSettingRepository bubbleSettingRepository;

	@Autowired
	WidgetItemStateRepository itemStateRepository;

	@Autowired
	WidgetDailySummaryRepository dailySummaryRepository;

	@Test
	@Transactional
	void widgetUpsertsAbsorbDuplicateFirstWrites() {
		User user = createUser("google-sub-widget-upsert", "미연");
		ProjectRoom room = projectRoomRepository.save(ProjectRoom.create(
				user.getId(),
				"위젯 컨텍스트룸",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		));

		contextSettingRepository.upsertContext(UUID.randomUUID(), user.getId(), null, WidgetMode.PERSONAL.name());
		contextSettingRepository.upsertContext(UUID.randomUUID(), user.getId(), room.getId(), WidgetMode.ROOM.name());
		var context = contextSettingRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(context.getSelectedRoomId()).isEqualTo(room.getId());
		assertThat(context.getMode()).isEqualTo(WidgetMode.ROOM);

		bubbleSettingRepository.insertDefaultIfAbsent(UUID.randomUUID(), user.getId(), BubbleType.TODO.name());
		var bubbleSetting = bubbleSettingRepository.findByUserIdAndBubbleType(user.getId(), BubbleType.TODO)
				.orElseThrow();

		UUID itemId = UUID.randomUUID();
		itemStateRepository.upsertState(
				UUID.randomUUID(),
				user.getId(),
				BubbleType.TODO.name(),
				WidgetItemType.TASK.name(),
				itemId,
				WidgetItemStateValue.PINNED.name()
		);
		itemStateRepository.upsertState(
				UUID.randomUUID(),
				user.getId(),
				BubbleType.TODO.name(),
				WidgetItemType.TASK.name(),
				itemId,
				WidgetItemStateValue.CONFIRMED.name()
		);
		var states = itemStateRepository.findByUserIdAndItemIdIn(user.getId(), List.of(itemId));
		assertThat(states).hasSize(1);
		assertThat(states.getFirst().getState()).isEqualTo(WidgetItemStateValue.CONFIRMED);

		LocalDate summaryDate = LocalDate.parse("2026-07-05");
		Instant syncedAt = Instant.parse("2026-07-05T01:00:00Z");
		dailySummaryRepository.insertIfAbsent(
				UUID.randomUUID(),
				user.getId(),
				"macbook",
				"rollup-1",
				summaryDate,
				bubbleSetting.getId(),
				1,
				2,
				30,
				syncedAt
		);
		dailySummaryRepository.insertIfAbsent(
				UUID.randomUUID(),
				user.getId(),
				"macbook",
				"rollup-1",
				summaryDate,
				bubbleSetting.getId(),
				99,
				99,
				99,
				syncedAt
		);
		dailySummaryRepository.insertIfAbsent(
				UUID.randomUUID(),
				user.getId(),
				"macbook",
				"rollup-2",
				summaryDate,
				bubbleSetting.getId(),
				99,
				99,
				99,
				syncedAt
		);
		var summaries = dailySummaryRepository.findByUserIdAndSummaryDate(user.getId(), summaryDate);
		assertThat(summaries).hasSize(1);
		assertThat(summaries.getFirst().getRollupKey()).isEqualTo("rollup-1");
		assertThat(summaries.getFirst().getOpenCount()).isEqualTo(1);
	}

	private User createUser(String googleSub, String name) {
		String bubliId = "bubli-" + UUID.randomUUID().toString().substring(0, 8);
		return userRepository.save(User.createGoogleUser(googleSub, bubliId, name, null, "ko-KR", "Asia/Seoul"));
	}
}
