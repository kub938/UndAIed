package com.ssafy.undaied.socket.chat.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.ssafy.undaied.socket.chat.dto.request.LobbyChatRequestDto;
import com.ssafy.undaied.socket.chat.dto.request.RoomChatRequestDto;
import com.ssafy.undaied.socket.chat.dto.response.RoomChatResponseDto;
import com.ssafy.undaied.socket.common.exception.SocketException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ssafy.undaied.socket.common.constant.EventType.*;
import static com.ssafy.undaied.socket.common.constant.SocketRoom.*;
import static com.ssafy.undaied.socket.common.exception.SocketErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SocketIOServer server;

    public void roomChat(RoomChatRequestDto requestDto, SocketIOClient client) throws SocketException {
        if(requestDto == null || client == null) {
            throw new SocketException(ROOM_CHAT_FAILED);
        }

        String key = ROOM_KEY_PREFIX + requestDto.getRoomId();

        RoomChatResponseDto roomChat = RoomChatResponseDto.builder()
                        .nickname(client.get("nickname"))
                        .message(requestDto.getMessage())
                        .build();

        server.getRoomOperations(key).sendEvent(ROOM_CHAT.getValue(), roomChat);

    }

    public void lobbyChat(LobbyChatRequestDto requestDto, SocketIOClient client) throws SocketException {
        if(requestDto == null || client == null) {
            log.info("request data null");
            throw new SocketException(LOBBY_CHAT_FAILED);
        }

        RoomChatResponseDto loobyChat = RoomChatResponseDto.builder()
                .nickname(client.get("nickname"))
                .message(requestDto.getMessage())
                .build();

        server.getRoomOperations(LOBBY_ROOM).sendEvent(LOBBY_CHAT.getValue(), loobyChat);
    }



}
