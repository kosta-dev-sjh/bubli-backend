package com.bubli.agent.controller;

import com.bubli.agent.dto.PersonalAgentCommandRequest;
import com.bubli.agent.dto.PersonalAgentCommandResponse;
import com.bubli.agent.service.PersonalAgentCommandService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PersonalAgentCommandController {

	private final PersonalAgentCommandService personalAgentCommandService;

	@PostMapping("/api/personal/agent/commands")
	public ApiResponse<PersonalAgentCommandResponse> execute(
			@CurrentUser AuthUser authUser,
			@Valid @RequestBody PersonalAgentCommandRequest request
	) {
		return ApiResponse.success(personalAgentCommandService.execute(
				authUser.userId(),
				request.message(),
				request.mode(),
				request.resourceIds(),
				request.memory().toInput()
		));
	}
}
