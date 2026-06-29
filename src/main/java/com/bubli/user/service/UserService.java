package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.dto.UpdateNotificationPreferencesCommand;
import com.bubli.user.dto.UpdatePrivacyConsentsCommand;
import com.bubli.user.dto.UpdateUserProfileCommand;
import com.bubli.user.dto.UpdateUserPreferenceCommand;
import com.bubli.user.dto.UserNotificationPreferenceResult;
import com.bubli.user.dto.UserNotificationPreferencesResult;
import com.bubli.user.dto.UserPreferenceResult;
import com.bubli.user.dto.UserPrivacyConsentResult;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final UserPreferenceRepository userPreferenceRepository;
	private final UserNotificationPreferenceRepository userNotificationPreferenceRepository;
	private final UserPrivacyConsentRepository userPrivacyConsentRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional(readOnly = true)
	public UserResult getMe(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		return UserResult.from(user);
	}

	@Transactional
	public UserResult updateMe(UUID userId, UpdateUserProfileCommand command) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		user.updateProfile(
				command.name(),
				command.avatarUrl(),
				command.locale(),
				command.timezone()
		);
		return UserResult.from(user);
	}

	@Transactional(readOnly = true)
	public UserPreferenceResult getPreferences(UUID userId) {
		return userPreferenceRepository.findByUserId(userId)
				.map(UserPreferenceResult::from)
				.orElseGet(() -> UserPreferenceResult.empty(userId));
	}

	@Transactional
	public UserPreferenceResult updatePreferences(UUID userId, UpdateUserPreferenceCommand command) {
		if (command.defaultProjectRoomId() != null) {
			projectMembershipPublicService.assertActiveMember(userId, command.defaultProjectRoomId());
		}
		UserPreference preference = userPreferenceRepository.findByUserId(userId)
				.orElseGet(() -> UserPreference.create(userId));
		preference.update(
				command.theme(),
				command.defaultHomeType(),
				command.defaultProjectRoomId()
		);
		return UserPreferenceResult.from(userPreferenceRepository.save(preference));
	}

	@Transactional(readOnly = true)
	public UserNotificationPreferencesResult getNotificationPreferences(UUID userId) {
		return toNotificationPreferencesResult(
				userId,
				userNotificationPreferenceRepository.findByIdUserId(userId)
		);
	}

	@Transactional
	public UserNotificationPreferencesResult updateNotificationPreferences(
			UUID userId,
			UpdateNotificationPreferencesCommand command
	) {
		Map<NotificationType, UserNotificationPreference> preferences =
				userNotificationPreferenceRepository.findByIdUserId(userId).stream()
						.collect(Collectors.toMap(
								UserNotificationPreference::getNotificationType,
								Function.identity()
						));

		List<UserNotificationPreference> changedPreferences = command.items().stream()
				.map(item -> {
					UserNotificationPreference preference = preferences.computeIfAbsent(
							item.notificationType(),
							notificationType -> UserNotificationPreference.create(userId, notificationType, true)
					);
					preference.updateEnabled(item.enabled());
					return preference;
				})
				.toList();

		userNotificationPreferenceRepository.saveAll(changedPreferences);
		return toNotificationPreferencesResult(userId, preferences.values().stream().toList());
	}

	private UserNotificationPreferencesResult toNotificationPreferencesResult(
			UUID userId,
			List<UserNotificationPreference> preferences
	) {
		Map<NotificationType, Boolean> enabledByType = Arrays.stream(NotificationType.values())
				.collect(Collectors.toMap(
						Function.identity(),
						ignored -> true,
						(left, right) -> right,
						LinkedHashMap::new
				));
		preferences.forEach(preference -> enabledByType.put(
				preference.getNotificationType(),
				preference.isEnabled()
		));
		List<UserNotificationPreferenceResult> items = enabledByType.entrySet().stream()
				.map(entry -> new UserNotificationPreferenceResult(entry.getKey(), entry.getValue()))
				.toList();
		return new UserNotificationPreferencesResult(userId, items);
	}

	@Transactional(readOnly = true)
	public UserPrivacyConsentsResult getPrivacyConsents(UUID userId) {
		return toPrivacyConsentsResult(
				userId,
				userPrivacyConsentRepository.findByIdUserId(userId)
		);
	}

	@Transactional
	public UserPrivacyConsentsResult updatePrivacyConsents(UUID userId, UpdatePrivacyConsentsCommand command) {
		Map<ConsentType, UserPrivacyConsent> consents =
				userPrivacyConsentRepository.findByIdUserId(userId).stream()
						.collect(Collectors.toMap(
								UserPrivacyConsent::getConsentType,
								Function.identity()
						));

		List<UserPrivacyConsent> changedConsents = command.items().stream()
				.map(item -> {
					UserPrivacyConsent consent = consents.computeIfAbsent(
							item.consentType(),
							consentType -> UserPrivacyConsent.create(userId, consentType, false)
					);
					consent.updateEnabled(item.enabled());
					return consent;
				})
				.toList();

		userPrivacyConsentRepository.saveAll(changedConsents);
		return toPrivacyConsentsResult(userId, consents.values().stream().toList());
	}

	private UserPrivacyConsentsResult toPrivacyConsentsResult(UUID userId, List<UserPrivacyConsent> consents) {
		Map<ConsentType, UserPrivacyConsent> consentByType = consents.stream()
				.collect(Collectors.toMap(
						UserPrivacyConsent::getConsentType,
						Function.identity()
				));
		List<UserPrivacyConsentResult> items = Arrays.stream(ConsentType.values())
				.map(consentType -> {
					UserPrivacyConsent consent = consentByType.get(consentType);
					return new UserPrivacyConsentResult(
							consentType,
							consent != null && consent.isEnabled(),
							consent == null ? null : consent.getUpdatedAt()
					);
				})
				.toList();
		return new UserPrivacyConsentsResult(userId, items);
	}
}
