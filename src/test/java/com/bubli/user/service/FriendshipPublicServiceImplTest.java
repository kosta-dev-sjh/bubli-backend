package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.repository.FriendshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FriendshipPublicServiceImplTest {

	@Mock
	FriendshipRepository friendshipRepository;

	@InjectMocks
	FriendshipPublicServiceImpl friendshipPublicService;

	@Test
	void assertAcceptedFriendPassesForExistingFriendship() {
		UUID userId = UUID.randomUUID();
		UUID friendUserId = UUID.randomUUID();
		given(friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)).willReturn(true);

		friendshipPublicService.assertAcceptedFriend(userId, friendUserId);
	}

	@Test
	void assertAcceptedFriendRejectsNonFriend() {
		UUID userId = UUID.randomUUID();
		UUID friendUserId = UUID.randomUUID();
		given(friendshipRepository.existsByUserIdAndFriendUserId(userId, friendUserId)).willReturn(false);

		assertThatThrownBy(() -> friendshipPublicService.assertAcceptedFriend(userId, friendUserId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROJECT_403_003));
	}
}
