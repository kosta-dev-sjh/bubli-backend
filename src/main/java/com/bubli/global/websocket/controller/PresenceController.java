package com.bubli.global.websocket.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.global.websocket.DesktopPresenceRegistry;
import com.bubli.global.websocket.dto.PresenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PresenceController {

	private final DesktopPresenceRegistry desktopPresenceRegistry;

	// 웹 탭이 수신 전화 팝업을 띄우기 전에, 같은 계정의 데스크톱 앱(위젯)이 이미 떠 있어서
	// 그쪽이 팝업을 전담할지 확인한다.
	@GetMapping("/api/presence/desktop-active")
	public ApiResponse<PresenceResponse> desktopActive(@CurrentUser AuthUser authUser) {
		return ApiResponse.success(new PresenceResponse(desktopPresenceRegistry.isActive(authUser.userId())));
	}
}
