package com.bubli.project.service;

import com.bubli.project.dto.ProjectRoomResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomPublicServiceImpl implements ProjectRoomPublicService {

	private static final int DEFAULT_LIMIT = 5;
	private static final int MAX_LIMIT = 20;

	private final ProjectRoomService projectRoomService;

	@Override
	@Transactional(readOnly = true)
	public List<ProjectRoomResult> getAccessibleRooms(UUID userId, int limit) {
		return projectRoomService.getProjectRooms(userId, PageRequest.of(0, boundedLimit(limit)))
				.getItems();
	}

	@Override
	@Transactional(readOnly = true)
	public ProjectRoomResult getProjectRoom(UUID userId, UUID roomId) {
		return projectRoomService.getProjectRoom(userId, roomId);
	}

	private int boundedLimit(int limit) {
		if (limit <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}
