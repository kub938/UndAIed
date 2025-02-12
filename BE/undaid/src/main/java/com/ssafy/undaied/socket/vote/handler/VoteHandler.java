package com.ssafy.undaied.socket.vote.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.ssafy.undaied.socket.common.exception.SocketException;
import com.ssafy.undaied.socket.common.exception.SocketExceptionHandler;
import com.ssafy.undaied.socket.common.constant.EventType;
import com.ssafy.undaied.socket.common.response.AckResponse;
import com.ssafy.undaied.socket.vote.dto.request.VoteSubmitRequestDto;
import com.ssafy.undaied.socket.vote.dto.response.VoteResultResponseDto;
import com.ssafy.undaied.socket.vote.dto.response.VoteSubmitResponseDto;
import com.ssafy.undaied.socket.vote.service.VoteService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteHandler {

    private final SocketIOServer server;
    private final VoteService voteService;
    private final SocketExceptionHandler socketExceptionHandler;

    @PostConstruct
    public void init() {
        server.addNamespace("/socket.io").addEventListener(EventType.SUBMIT_VOTE.getValue(), VoteSubmitRequestDto.class,
                (client, data, ack) -> {
                    try {
                        Integer userId = client.get("userId");
                        Integer gameId = client.get("gameId");

                        VoteSubmitResponseDto responseDto = voteService.submitVote(userId, gameId, data);
                        if (ack.isAckRequested()) {
                            ack.sendAckData(new AckResponse(true, null, responseDto));
                        }
                    } catch (SocketException e) {
                        log.error("SocketException in submitVote: {}", e.getMessage());
                        if (ack.isAckRequested()) {
                            ack.sendAckData(new AckResponse(false, e.getErrorCode().getMessage(), null));
                        }
                    } catch (Exception e) {
                        log.error("Unexpected error in submitVote: {}", e.getMessage());
                        if (ack.isAckRequested()) {
                            ack.sendAckData(new AckResponse(false, "Unexpected error occurred", null));
                        }
                    }
                });
    }


    // 투표 제출
    public void submitVote(SocketIOClient client, VoteSubmitRequestDto voteSubmitRequestDto) {
        Integer userId = client.get("userId");
        Integer gameId = 1; // 테스트를 위해 임의로 설정
//        Integer gameId = client.get("gameId");
//        VoteSubmitResponseDto response = voteService.submitVote(userId, gameId, voteSubmitRequestDto);
    }

    // 투표 결과 알림
    public void notifyVoteResult(Integer gameId) {
        VoteResultResponseDto responseDto = voteService.computeVoteResult(gameId);
        server.getRoomOperations(String.valueOf(gameId)).sendEvent(EventType.SHOW_VOTE_RESULT.getValue(), responseDto);
    }

}
