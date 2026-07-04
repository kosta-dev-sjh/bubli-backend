package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipPublicServiceImpl implements FriendshipPublicService {

	private final FriendshipRepository friendshipRepository;

	@Override
	@Transactional(readOnly = true)
	public void assertAcceptedFriend(UUID userId, UUID friendUserId) {
		if (!friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)) {
			throw new BusinessException(ErrorCode.PROJECT_403_003);
		}
	}
}
