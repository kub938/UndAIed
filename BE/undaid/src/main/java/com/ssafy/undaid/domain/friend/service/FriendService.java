package com.ssafy.undaid.domain.friend.service;

import com.ssafy.undaid.domain.friend.dto.request.FriendCreateRequestDto;
import com.ssafy.undaid.domain.friend.dto.request.FriendUpdateStatusRequestDto;
import com.ssafy.undaid.domain.friend.dto.response.FriendRequestListResponseDto;
import com.ssafy.undaid.domain.friend.dto.response.FriendResponseDto;
import com.ssafy.undaid.domain.friend.dto.response.FriendUpdateResponseDto;
import com.ssafy.undaid.domain.friend.entity.Friends;
import com.ssafy.undaid.domain.friend.entity.FriendshipStatus;
import com.ssafy.undaid.domain.friend.entity.repository.FriendRepository;
import com.ssafy.undaid.domain.user.entity.Users;
import com.ssafy.undaid.domain.user.service.UserService;
import com.ssafy.undaid.global.common.exception.BaseException;
import com.ssafy.undaid.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserService userService;

    public List<FriendResponseDto> getFriendsList(Integer userId) {
        System.out.println(userId);
        return friendRepository.findByUserId(userId);
    }

    public List<FriendRequestListResponseDto> getFriendRequestsList(Integer userId) {
        return friendRepository.findPendingByUserId(userId);
    }

    public void createFriend(Integer userId, FriendCreateRequestDto friendCreateRequestDto) {
        Users user = userService.getUserById(userId);   // 친구 요청을 보낸 사용자
        Users friendUser = userService.getUserByNickname(friendCreateRequestDto.getNickname()); // 친구 요청을 받은 사용자

        Friends friendship = friendRepository.findByUserIdAndFriendId(userId, friendUser.getUserId());
        if ( friendship != null) {
            switch (friendship.getStatus()) {
                case BLOCKED: throw new BaseException(ErrorCode.USER_NOT_FOUND);
                case ACCEPTED:
                case PENDING:
                    throw new BaseException(ErrorCode.FRIENDSHIP_ALREADY_REQUESTED);
                case DELETED:
                    friendship.updateStatus(FriendshipStatus.PENDING);
                    if (friendship.getUser().getUserId() != userId) {
                        friendship.updateDirection(user, friendUser);
                    }
                    return;
            }
        }

        Friends friend = Friends.builder()
                .status(FriendshipStatus.PENDING)
                .user(user)
                .friendUser(friendUser)
                .build();

        friendRepository.save(friend);
    }

    public FriendUpdateResponseDto updateFriendStatus(
            Integer userId, FriendUpdateStatusRequestDto friendUpdateStatusRequestDto) {
        Friends friendship = friendRepository.findByUserIdAndFriendId(userId, friendUpdateStatusRequestDto.getFriendId());

        friendship.updateStatus(friendUpdateStatusRequestDto.getStatus());

        Friends updatedFriend = friendRepository.save(friendship);

        return FriendUpdateResponseDto.builder()
                .status(updatedFriend.getStatus())
                .updatedAt(updatedFriend.getUpdatedAt())
                .userId(updatedFriend.getUser().getUserId())
                .friendId(updatedFriend.getFriendUser().getUserId())
                .build();
    }

}
