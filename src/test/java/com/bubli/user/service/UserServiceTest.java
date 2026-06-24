package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@InjectMocks
	UserService userService;

	@Test
	void getMeReturnsCurrentUserProfile() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.getMe(userId, "user@example.com");

		assertThat(result.id()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("user@example.com");
		assertThat(result.name()).isEqualTo("정현");
	}

	@Test
	void getMeThrowsWhenUserDoesNotExist() {
		UUID userId = UUID.randomUUID();
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMe(userId, "user@example.com"))
				.isInstanceOf(BusinessException.class);
	}
}
