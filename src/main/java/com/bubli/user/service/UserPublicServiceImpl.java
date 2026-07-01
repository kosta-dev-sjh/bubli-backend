package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.dto.UpsertGoogleUserCommand;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.entity.UserPrivacyConsentId;
import com.bubli.user.repository.UserRepository;
import com.bubli.global.locale.SupportedLocale;
import com.bubli.user.repository.UserPrivacyConsentRepository;
import com.bubli.user.type.ConsentType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPublicServiceImpl implements UserPublicService {

	private final UserRepository userRepository;
	private final UserPrivacyConsentRepository userPrivacyConsentRepository;
	private final BubliIdGenerator bubliIdGenerator;

	@Override
	@Transactional(readOnly = true)
	public UserResult getUser(UUID userId) {
		User user = getActiveUser(userId);
		return UserResult.from(user);
	}

	@Override
	@Transactional
	public UserResult upsertGoogleUser(UpsertGoogleUserCommand command) {
		User user = userRepository.findByGoogleSub(command.googleSub())
				.map(existingUser -> {
					if (!existingUser.isActive()) {
						throw new BusinessException(ErrorCode.USER_410_001);
					}
					return existingUser;
				})
				.orElseGet(() -> userRepository.save(User.createGoogleUser(
						command.googleSub(),
						generateBubliId(command),
						resolveName(command),
						command.avatarUrl(),
						SupportedLocale.normalize(command.locale()),
						command.timezone()
		)));
		user.updateProfile(
				resolveName(command),
				command.avatarUrl(),
				SupportedLocale.normalize(command.locale()),
				command.timezone()
		);
		return UserResult.from(user);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, UserResult> getUsers(Page<UUID> userIds) {
		return userRepository.findAllById(userIds.getContent()).stream()
				.filter(User::isActive)
				.map(this::toPublicResult)
				.collect(Collectors.toMap(UserResult::id, Function.identity()));
	}

	@Override
	@Transactional(readOnly = true)
	public void assertExists(UUID userId) {
		getActiveUser(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isPrivacyConsentEnabled(UUID userId, ConsentType consentType) {
		if (userRepository.findById(userId).filter(User::isActive).isEmpty()) {
			return false;
		}
		return userPrivacyConsentRepository.findById(UserPrivacyConsentId.of(userId, consentType))
				.map(consent -> consent.isEnabled())
				.orElse(false);
	}

	private User getActiveUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.USER_410_001);
		}
		return user;
	}

	private UserResult toPublicResult(User user) {
		return UserResult.from(user);
	}

	private String generateBubliId(UpsertGoogleUserCommand command) {
		for (int attempt = 0; attempt < bubliIdGenerator.maxAttempts(); attempt++) {
			String candidate = bubliIdGenerator.generate(command.googleSub(), attempt);
			if (!userRepository.existsByBubliId(candidate)) {
				return candidate;
			}
		}
		throw new BusinessException(ErrorCode.COMMON_500_001);
	}

	private String resolveName(UpsertGoogleUserCommand command) {
		if (hasText(command.name())) {
			return command.name();
		}
		return "Bubli User";
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
