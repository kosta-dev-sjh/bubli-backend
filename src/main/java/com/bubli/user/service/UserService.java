package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.user.dto.UpdateUserProfileCommand;
import com.bubli.user.dto.UpdateUserPreferenceCommand;
import com.bubli.user.dto.UserPreferenceResult;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.entity.UserPreference;
import com.bubli.user.repository.UserPreferenceRepository;
import com.bubli.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final UserPreferenceRepository userPreferenceRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

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
}
