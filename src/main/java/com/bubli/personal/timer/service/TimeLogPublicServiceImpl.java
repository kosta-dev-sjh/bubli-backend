package com.bubli.personal.timer.service;

import com.bubli.personal.timer.dto.TimeLogResult;
import com.bubli.personal.timer.repository.TimeLogRepository;
import com.bubli.personal.timer.type.TimeLogStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeLogPublicServiceImpl implements TimeLogPublicService {

	private final TimeLogRepository timeLogRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<TimeLogResult> getRunningTimer(UUID userId) {
		return timeLogRepository.findFirstByUserIdAndStatus(userId, TimeLogStatus.RUNNING)
				.map(TimeLogResult::from);
	}
}
