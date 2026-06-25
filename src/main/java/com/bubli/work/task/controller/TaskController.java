package com.bubli.work.task.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.work.task.dto.CreatePersonalTaskRequest;
import com.bubli.work.task.dto.CreateRoomTaskRequest;
import com.bubli.work.task.dto.TaskResponse;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.dto.UpdateTaskRequest;
import com.bubli.work.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController {

	private final TaskService taskService;

	@GetMapping("/api/tasks")
	public ApiResponse<PageResponse<TaskResponse>> getTasks(
			@CurrentUser AuthUser authUser,
			@RequestParam(defaultValue = "personal") String scope,
			@PageableDefault(size = 20) Pageable pageable
	) {
		if ("assigned".equalsIgnoreCase(scope)) {
			return ApiResponse.success(mapPage(taskService.getAssignedTasks(authUser.userId(), pageable)));
		}
		return ApiResponse.success(mapPage(taskService.getPersonalTasks(authUser.userId(), pageable)));
	}

	@GetMapping("/api/dashboard/tasks")
	public ApiResponse<PageResponse<TaskResponse>> getDashboardTasks(
			@CurrentUser AuthUser authUser,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(taskService.getDashboardTasks(authUser.userId(), pageable)));
	}

	@PostMapping("/api/tasks")
	public ApiResponse<TaskResponse> createPersonalTask(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody CreatePersonalTaskRequest request
	) {
		return ApiResponse.success(TaskResponse.from(taskService.createPersonalTask(authUser.userId(), request)));
	}

	@GetMapping("/api/project-rooms/{roomId}/tasks")
	public ApiResponse<PageResponse<TaskResponse>> getRoomTasks(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(mapPage(taskService.getRoomTasks(authUser.userId(), roomId, pageable)));
	}

	@PostMapping("/api/project-rooms/{roomId}/tasks")
	public ApiResponse<TaskResponse> createRoomTask(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID roomId,
			@Valid @RequestBody CreateRoomTaskRequest request
	) {
		return ApiResponse.success(TaskResponse.from(taskService.createRoomTask(authUser.userId(), roomId, request)));
	}

	@PatchMapping("/api/tasks/{taskId}")
	public ApiResponse<TaskResponse> updateTask(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID taskId,
			@Valid @RequestBody UpdateTaskRequest request
	) {
		return ApiResponse.success(TaskResponse.from(taskService.updateTask(authUser.userId(), taskId, request)));
	}

	@DeleteMapping("/api/tasks/{taskId}")
	public ApiResponse<Void> deleteTask(
			@CurrentUser AuthUser authUser,
			@PathVariable UUID taskId
	) {
		taskService.deleteTask(authUser.userId(), taskId);
		return ApiResponse.success(null);
	}

	private PageResponse<TaskResponse> mapPage(PageResponse<TaskResult> page) {
		return new PageResponse<>(
				page.getItems().stream()
						.map(TaskResponse::from)
						.toList(),
				page.getPage(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isHasNext()
		);
	}
}
