package com.bubli.work.schedule.service;

import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulePublicServiceImpl implements SchedulePublicService {

	private final ScheduleRepository scheduleRepository;

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleResult> getSchedulesBetween(UUID userId, Instant from, Instant to) {
		return scheduleRepository.findByOwnerUserIdAndStartsAtBetweenOrderByStartsAtAsc(userId, from, to)
				.stream()
				.map(ScheduleResult::from)
				.toList();
	}
}
