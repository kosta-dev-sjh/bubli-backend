package com.bubli.user.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.dto.FriendRequestResponse;
import com.bubli.user.dto.FriendResponse;
import com.bubli.user.dto.UserSearchResponse;
import com.bubli.user.entity.FriendRequest;
import com.bubli.user.entity.Friendship;
import com.bubli.user.entity.User;
import com.bubli.user.repository.FriendRequestRepository;
import com.bubli.user.repository.FriendshipRepository;
import com.bubli.user.repository.UserRepository;
import com.bubli.user.type.FriendRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

	private final FriendRequestRepository friendRequestRepository;
	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<FriendResponse> getFriends(UUID userId) {
		List<Friendship> friendships = friendshipRepository.findByUserId(userId);
		List<UUID> friendIds = friendships.stream().map(Friendship::getFriendUserId).toList();
		Map<UUID, User> userMap = userRepository.findAllById(friendIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));

		return friendships.stream()
				.filter(f -> userMap.containsKey(f.getFriendUserId()))
				.map(f -> {
					User friend = userMap.get(f.getFriendUserId());
					return new FriendResponse(friend.getId(), friend.getBubliId(), friend.getName(), friend.getAvatarUrl(), f.getAcceptedAt());
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public UserSearchResponse searchByBubliId(UUID requesterId, String bubliId) {
		User user = userRepository.findByBubliId(bubliId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));
		if (user.getId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.USER_400_001);
		}
		return new UserSearchResponse(user.getId(), user.getBubliId(), user.getName(), user.getAvatarUrl());
	}

	@Transactional
	public void deleteFriend(UUID userId, UUID friendUserId) {
		friendshipRepository.deleteByUserIdAndFriendUserId(userId, friendUserId);
		friendshipRepository.deleteByUserIdAndFriendUserId(friendUserId, userId);
	}

	@Transactional(readOnly = true)
	public List<FriendRequestResponse> getFriendRequests(UUID userId) {
		List<FriendRequest> sent = friendRequestRepository.findByRequesterIdAndStatus(userId, FriendRequestStatus.PENDING);
		List<FriendRequest> received = friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);

		List<UUID> allUserIds = new ArrayList<>();
		sent.forEach(r -> { allUserIds.add(r.getRequesterId()); allUserIds.add(r.getReceiverId()); });
		received.forEach(r -> { allUserIds.add(r.getRequesterId()); allUserIds.add(r.getReceiverId()); });

		Map<UUID, User> userMap = userRepository.findAllById(allUserIds).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));

		List<FriendRequestResponse> result = new ArrayList<>();
		sent.stream().map(r -> toResponse(r, userMap)).forEach(result::add);
		received.stream().map(r -> toResponse(r, userMap)).forEach(result::add);
		return result;
	}

	@Transactional
	public FriendRequestResponse sendFriendRequest(UUID requesterId, String targetBubliId) {
		User receiver = userRepository.findByBubliId(targetBubliId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));

		if (receiver.getId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.USER_400_001);
		}
		if (friendshipRepository.existsByUserIdAndFriendUserId(requesterId, receiver.getId())) {
			throw new BusinessException(ErrorCode.USER_409_002);
		}
		if (friendRequestRepository.existsByRequesterIdAndReceiverIdAndStatus(requesterId, receiver.getId(), FriendRequestStatus.PENDING)) {
			throw new BusinessException(ErrorCode.USER_409_001);
		}

		FriendRequest saved = friendRequestRepository.save(FriendRequest.create(requesterId, receiver.getId()));
		User requester = userRepository.findById(requesterId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_001));

		return new FriendRequestResponse(
				saved.getId(), saved.getRequesterId(), requester.getName(), requester.getBubliId(),
				saved.getReceiverId(), receiver.getName(), receiver.getBubliId(),
				saved.getStatus().name(), saved.getCreatedAt()
		);
	}

	@Transactional
	public FriendRequestResponse acceptFriendRequest(UUID userId, UUID requestId) {
		FriendRequest request = friendRequestRepository.findByIdAndReceiverId(requestId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_002));

		if (request.getStatus() != FriendRequestStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_409_003);
		}

		request.accept();
		friendshipRepository.save(Friendship.create(userId, request.getRequesterId()));
		friendshipRepository.save(Friendship.create(request.getRequesterId(), userId));

		return buildResponse(request);
	}

	@Transactional
	public FriendRequestResponse rejectFriendRequest(UUID userId, UUID requestId) {
		FriendRequest request = friendRequestRepository.findByIdAndReceiverId(requestId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_404_002));

		if (request.getStatus() != FriendRequestStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_409_003);
		}

		request.reject();
		return buildResponse(request);
	}

	private FriendRequestResponse buildResponse(FriendRequest request) {
		Map<UUID, User> userMap = userRepository.findAllById(
				List.of(request.getRequesterId(), request.getReceiverId())
		).stream().collect(Collectors.toMap(User::getId, Function.identity()));

		return toResponse(request, userMap);
	}

	private FriendRequestResponse toResponse(FriendRequest request, Map<UUID, User> userMap) {
		User requester = userMap.get(request.getRequesterId());
		User receiver = userMap.get(request.getReceiverId());
		return new FriendRequestResponse(
				request.getId(),
				request.getRequesterId(),
				requester != null ? requester.getName() : null,
				requester != null ? requester.getBubliId() : null,
				request.getReceiverId(),
				receiver != null ? receiver.getName() : null,
				receiver != null ? receiver.getBubliId() : null,
				request.getStatus().name(),
				request.getCreatedAt()
		);
	}
}
