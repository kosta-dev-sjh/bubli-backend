package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.user.dto.UpsertGoogleUserCommand;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserPrivacyConsentRepository;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserPublicServiceImplTest {

	@Mock
	UserRepository userRepository;

	@Mock
	UserPrivacyConsentRepository userPrivacyConsentRepository;

	@Mock
	BubliIdGenerator bubliIdGenerator;

	@InjectMocks
	UserPublicServiceImpl userPublicService;

	@Test
	void upsertGoogleUserUsesGeneratedBubliId() {
		UUID userId = UUID.randomUUID();
		given(userRepository.findByGoogleSub("google-sub")).willReturn(Optional.empty());
		given(bubliIdGenerator.maxAttempts()).willReturn(1);
		given(bubliIdGenerator.generate("google-sub", 0)).willReturn("milo4827");
		given(userRepository.existsByBubliId("milo4827")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", userId);
			return user;
		});

		UserResult result = userPublicService.upsertGoogleUser(new UpsertGoogleUserCommand(
				"google-sub",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		));

		assertThat(result.bubliId()).isEqualTo("milo4827");
		assertThat(result.locale()).isEqualTo("ko-KR");
	}

	@Test
	void upsertGoogleUserRetriesNumericPartWhenBubliIdAlreadyExists() {
		UUID userId = UUID.randomUUID();
		given(userRepository.findByGoogleSub("google-sub")).willReturn(Optional.empty());
		given(bubliIdGenerator.maxAttempts()).willReturn(2);
		given(bubliIdGenerator.generate("google-sub", 0)).willReturn("milo4827");
		given(bubliIdGenerator.generate("google-sub", 1)).willReturn("nora4828");
		given(userRepository.existsByBubliId("milo4827")).willReturn(true);
		given(userRepository.existsByBubliId("nora4828")).willReturn(false);
		given(userRepository.save(any(User.class))).willAnswer(invocation -> {
			User user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", userId);
			return user;
		});

		userPublicService.upsertGoogleUser(new UpsertGoogleUserCommand(
				"google-sub",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getBubliId()).isEqualTo("nora4828");
		assertThat(userCaptor.getValue().getLocale()).isEqualTo("ko-KR");
	}

	@Test
	void upsertGoogleUserRejectsWithdrawnUser() {
		User user = User.createGoogleUser("google-sub", "bubli-id", "미연", null, "ko", "Asia/Seoul");
		user.withdraw();
		given(userRepository.findByGoogleSub("google-sub")).willReturn(Optional.of(user));

		assertThatThrownBy(() -> userPublicService.upsertGoogleUser(new UpsertGoogleUserCommand(
				"google-sub",
				"미연",
				null,
				"ko",
				"Asia/Seoul"
		))).isInstanceOf(BusinessException.class);
	}

	@Test
	void getUsersDeduplicatesIdsAndFiltersInactiveUsers() {
		UUID activeUserId = UUID.randomUUID();
		UUID withdrawnUserId = UUID.randomUUID();
		User activeUser = user(activeUserId, "active-sub", "active-id", "미연");
		User withdrawnUser = user(withdrawnUserId, "withdrawn-sub", "withdrawn-id", "탈퇴");
		withdrawnUser.withdraw();
		given(userRepository.findAllById(List.of(activeUserId, withdrawnUserId)))
				.willReturn(List.of(activeUser, withdrawnUser));

		Map<UUID, UserResult> result = userPublicService.getUsers(Arrays.asList(
				activeUserId,
				null,
				activeUserId,
				withdrawnUserId
		));

		assertThat(result).containsOnlyKeys(activeUserId);
		assertThat(result.get(activeUserId).name()).isEqualTo("미연");
		verify(userRepository).findAllById(List.of(activeUserId, withdrawnUserId));
	}

	@Test
	void getUsersReturnsEmptyMapWithoutRepositoryCallWhenNoValidIds() {
		Map<UUID, UserResult> result = userPublicService.getUsers(Arrays.asList((UUID) null));

		assertThat(result).isEmpty();
		verifyNoInteractions(userRepository);
	}

	private User user(UUID userId, String googleSub, String bubliId, String name) {
		User user = User.createGoogleUser(googleSub, bubliId, name, null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
