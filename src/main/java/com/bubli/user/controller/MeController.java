package com.bubli.user.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.user.dto.MeResponse;
import com.bubli.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

	private final UserService userService;

	@GetMapping("/api/me")
	public ApiResponse<MeResponse> getMe(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(MeResponse.from(userService.getMe(authUser.userId(), authUser.email())));
	}
}
