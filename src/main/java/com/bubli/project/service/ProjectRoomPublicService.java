package com.bubli.project.service;

import com.bubli.project.dto.ProjectRoomResult;

import java.util.List;
import java.util.UUID;

public interface ProjectRoomPublicService {

	List<ProjectRoomResult> getAccessibleRooms(UUID userId, int limit);
}
