package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.dto.UserResult;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
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

	@Override
	@Transactional(readOnly = true)
	public UserResult getUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		return toPublicResult(user);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<UUID, UserResult> getUsers(Page<UUID> userIds) {
		return userRepository.findAllById(userIds.getContent()).stream()
				.map(this::toPublicResult)
				.collect(Collectors.toMap(UserResult::id, Function.identity()));
	}

	@Override
	@Transactional(readOnly = true)
	public void assertExists(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new BusinessException(ErrorCode.USER_404_001);
		}
	}

	private UserResult toPublicResult(User user) {
		return UserResult.from(user, null);
	}
}
