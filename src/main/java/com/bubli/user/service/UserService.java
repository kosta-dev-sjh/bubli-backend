package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectRoomService;
import com.bubli.user.dto.UpdateNotificationPreferencesCommand;
import com.bubli.user.dto.UpdateUserProfileCommand;
import com.bubli.user.dto.UpdateUserPreferenceCommand;
import com.bubli.user.dto.UserNotificationPreferenceResult;
import com.bubli.user.dto.UserNotificationPreferencesResult;
import com.bubli.user.dto.UserPreferenceResult;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.entity.UserNotificationPreference;
import com.bubli.user.entity.UserPreference;
import com.bubli.user.repository.UserNotificationPreferenceRepository;
import com.bubli.user.repository.UserPreferenceRepository;
import com.bubli.user.repository.UserRepository;
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
	private final ProjectRoomService projectRoomService;

	@Transactional(readOnly = true)
	public UserResult getMe(UUID userId, String email) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		return UserResult.from(user, email);
	}

	@Transactional
	public UserResult updateMe(UUID userId, String email, UpdateUserProfileCommand command) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		user.updateProfile(
				command.name(),
				command.avatarUrl(),
				command.locale(),
				command.timezone()
		);
		return UserResult.from(user, email);
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
			projectRoomService.getProjectRoom(userId, command.defaultProjectRoomId());
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
}
