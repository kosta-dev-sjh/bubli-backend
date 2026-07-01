package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.dto.UpdateNotificationPreferencesCommand;
import com.bubli.user.dto.UpdatePrivacyConsentsCommand;
import com.bubli.user.dto.UpdateUserProfileCommand;
import com.bubli.user.dto.UpdateUserPreferenceCommand;
import com.bubli.user.dto.UserNotificationPreferencesResult;
import com.bubli.user.dto.UserPreferenceResult;
import com.bubli.user.dto.UserPrivacyConsentsResult;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.entity.UserNotificationPreference;
import com.bubli.user.entity.UserPreference;
import com.bubli.user.entity.UserPrivacyConsent;
import com.bubli.user.repository.UserNotificationPreferenceRepository;
import com.bubli.user.repository.UserPreferenceRepository;
import com.bubli.user.repository.UserPrivacyConsentRepository;
import com.bubli.user.repository.UserRepository;
import com.bubli.user.type.ConsentType;
import com.bubli.user.type.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	UserPreferenceRepository userPreferenceRepository;

	@Mock
	UserNotificationPreferenceRepository userNotificationPreferenceRepository;

	@Mock
	UserPrivacyConsentRepository userPrivacyConsentRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@InjectMocks
	UserService userService;

	@Test
	void getMeReturnsCurrentUserProfile() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "ko", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.getMe(userId);

		assertThat(result.id()).isEqualTo(userId);
		assertThat(result.name()).isEqualTo("정현");
	}

	@Test
	void getMeThrowsWhenUserDoesNotExist() {
		UUID userId = UUID.randomUUID();
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMe(userId))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void updateMeChangesProvidedProfileFields() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "ko-KR", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.updateMe(userId, new UpdateUserProfileCommand(
				"마렌",
				"https://cdn.example/avatar.png",
				"ja-JP",
				"Asia/Tokyo"
		));

		assertThat(result.id()).isEqualTo(userId);
		assertThat(result.name()).isEqualTo("마렌");
		assertThat(result.avatarUrl()).isEqualTo("https://cdn.example/avatar.png");
		assertThat(result.locale()).isEqualTo("ja-JP");
		assertThat(result.timezone()).isEqualTo("Asia/Tokyo");
	}

	@Test
	void updateMeNormalizesProvidedLocale() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "en-US", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.updateMe(userId, new UpdateUserProfileCommand(
				null,
				null,
				"ja",
				null
		));

		assertThat(result.locale()).isEqualTo("ja-JP");
	}

	@Test
	void updateMeKeepsLocaleWhenLocaleIsOmitted() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "en-US", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.updateMe(userId, new UpdateUserProfileCommand(
				"new name",
				null,
				null,
				null
		));

		assertThat(result.locale()).isEqualTo("en-US");
	}

	@Test
	void getMeFallsBackToDefaultLocaleWhenStoredLocaleIsUnsupported() {
		UUID userId = UUID.randomUUID();
		User user = User.createGoogleUser("google-sub", "bubli", "정현", null, "fr-FR", "Asia/Seoul");
		ReflectionTestUtils.setField(user, "id", userId);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		UserResult result = userService.getMe(userId);

		assertThat(result.locale()).isEqualTo("ko-KR");
	}

	@Test
	void getPreferencesReturnsEmptyResultWhenPreferenceDoesNotExist() {
		UUID userId = UUID.randomUUID();
		given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.empty());

		UserPreferenceResult result = userService.getPreferences(userId);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.theme()).isNull();
		assertThat(result.defaultHomeType()).isNull();
		assertThat(result.defaultProjectRoomId()).isNull();
	}

	@Test
	void updatePreferencesValidatesDefaultProjectRoomAndSavesPreference() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID preferenceId = UUID.randomUUID();
		given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.empty());
		given(userPreferenceRepository.save(org.mockito.ArgumentMatchers.any(UserPreference.class)))
				.willAnswer(invocation -> {
					UserPreference preference = invocation.getArgument(0);
					ReflectionTestUtils.setField(preference, "id", preferenceId);
					return preference;
				});

		UserPreferenceResult result = userService.updatePreferences(userId, new UpdateUserPreferenceCommand(
				"LIGHT",
				"PROJECT_ROOM",
				roomId
		));

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.theme()).isEqualTo("LIGHT");
		assertThat(result.defaultHomeType()).isEqualTo("PROJECT_ROOM");
		assertThat(result.defaultProjectRoomId()).isEqualTo(roomId);
		org.mockito.Mockito.verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
	}

	@Test
	void getNotificationPreferencesReturnsAllTypesEnabledByDefault() {
		UUID userId = UUID.randomUUID();
		given(userNotificationPreferenceRepository.findByIdUserId(userId)).willReturn(List.of());

		UserNotificationPreferencesResult result = userService.getNotificationPreferences(userId);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.items())
				.extracting(item -> item.notificationType())
				.containsExactly(NotificationType.values());
		assertThat(result.items())
				.allSatisfy(item -> assertThat(item.enabled()).isTrue());
	}

	@Test
	void updateNotificationPreferencesUpsertsRequestedTypes() {
		UUID userId = UUID.randomUUID();
		UserNotificationPreference existingPreference = UserNotificationPreference.create(
				userId,
				NotificationType.MESSAGE,
				true
		);
		given(userNotificationPreferenceRepository.findByIdUserId(userId)).willReturn(List.of(existingPreference));

		UserNotificationPreferencesResult result = userService.updateNotificationPreferences(
				userId,
				new UpdateNotificationPreferencesCommand(List.of(
						new UpdateNotificationPreferencesCommand.Item(NotificationType.MESSAGE, false),
						new UpdateNotificationPreferencesCommand.Item(NotificationType.AGENT, true)
				))
		);

		assertThat(result.items())
				.filteredOn(item -> item.notificationType() == NotificationType.MESSAGE)
				.singleElement()
				.satisfies(item -> assertThat(item.enabled()).isFalse());
		assertThat(result.items())
				.filteredOn(item -> item.notificationType() == NotificationType.AGENT)
				.singleElement()
				.satisfies(item -> assertThat(item.enabled()).isTrue());
		org.mockito.Mockito.verify(userNotificationPreferenceRepository)
				.saveAll(ArgumentMatchers.argThat(preferences -> {
					List<UserNotificationPreference> savedPreferences = StreamSupport.stream(
							preferences.spliterator(),
							false
					).toList();
					return savedPreferences.size() == 2
							&& savedPreferences.stream()
							.anyMatch(preference -> preference.getNotificationType() == NotificationType.MESSAGE
									&& !preference.isEnabled())
							&& savedPreferences.stream()
							.anyMatch(preference -> preference.getNotificationType() == NotificationType.AGENT
									&& preference.isEnabled());
				}));
	}

	@Test
	void getPrivacyConsentsReturnsAllTypesDisabledByDefault() {
		UUID userId = UUID.randomUUID();
		given(userPrivacyConsentRepository.findByIdUserId(userId)).willReturn(List.of());

		UserPrivacyConsentsResult result = userService.getPrivacyConsents(userId);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.items())
				.extracting(item -> item.consentType())
				.containsExactly(ConsentType.values());
		assertThat(result.items())
				.allSatisfy(item -> {
					assertThat(item.enabled()).isFalse();
					assertThat(item.updatedAt()).isNull();
				});
	}

	@Test
	void updatePrivacyConsentsUpsertsRequestedTypes() {
		UUID userId = UUID.randomUUID();
		UserPrivacyConsent existingConsent = UserPrivacyConsent.create(
				userId,
				ConsentType.ACTIVITY_CONTEXT,
				false
		);
		given(userPrivacyConsentRepository.findByIdUserId(userId)).willReturn(List.of(existingConsent));

		UserPrivacyConsentsResult result = userService.updatePrivacyConsents(
				userId,
				new UpdatePrivacyConsentsCommand(List.of(
						new UpdatePrivacyConsentsCommand.Item(ConsentType.ACTIVITY_CONTEXT, true),
						new UpdatePrivacyConsentsCommand.Item(ConsentType.MANAGED_FOLDER, false)
				))
		);

		assertThat(result.items())
				.filteredOn(item -> item.consentType() == ConsentType.ACTIVITY_CONTEXT)
				.singleElement()
				.satisfies(item -> {
					assertThat(item.enabled()).isTrue();
					assertThat(item.updatedAt()).isNotNull();
				});
		assertThat(result.items())
				.filteredOn(item -> item.consentType() == ConsentType.MANAGED_FOLDER)
				.singleElement()
				.satisfies(item -> {
					assertThat(item.enabled()).isFalse();
					assertThat(item.updatedAt()).isNotNull();
				});
		org.mockito.Mockito.verify(userPrivacyConsentRepository)
				.saveAll(ArgumentMatchers.argThat(consents -> {
					List<UserPrivacyConsent> savedConsents = StreamSupport.stream(
							consents.spliterator(),
							false
					).toList();
					return savedConsents.size() == 2
							&& savedConsents.stream()
							.anyMatch(consent -> consent.getConsentType() == ConsentType.ACTIVITY_CONTEXT
									&& consent.isEnabled())
							&& savedConsents.stream()
							.anyMatch(consent -> consent.getConsentType() == ConsentType.MANAGED_FOLDER
									&& !consent.isEnabled());
				}));
	}
}
