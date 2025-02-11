package com.ssafy.undaied.socket.lobby.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.ssafy.undaied.socket.lobby.dto.response.LobbyUpdateResponseDto;
import com.ssafy.undaied.socket.lobby.dto.response.UpdateData;
import com.ssafy.undaied.socket.room.dto.response.RoomCreateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

import static com.ssafy.undaied.socket.common.constant.SocketRoom.LOBBY_ROOM;

@Service
@Slf4j
@RequiredArgsConstructor
public class LobbyService {
    /**
     * 클라이언트를 로비에 입장시킵니다.
     */
    public void joinLobby(SocketIOClient client) {
        log.debug("Attempting to join lobby - Client ID: {}", client.getSessionId());
        log.debug("Current rooms before joining lobby: {}", client.getAllRooms());

        // 기존 방에서 모두 나가기
        Set<String> rooms = new HashSet<>(client.getAllRooms());
        rooms.remove("");
        for (String room : rooms) {
            client.leaveRoom(room);
            log.debug("Left room: {}", room);
        }

        // 로비에 입장
        client.joinRoom(LOBBY_ROOM);
        log.debug("Joined lobby - Current rooms: {}", client.getAllRooms());
    }

    /**
     * 클라이언트를 로비에서 퇴장시킵니다.
     */
    public void leaveLobby(SocketIOClient client) {
        client.leaveRoom(LOBBY_ROOM);
        log.info("User {} (sessionId: {}) left lobby", client.get("userId"), client.getSessionId());
    }

    public boolean isUserInLobby(SocketIOClient client) {
        Set<String> rooms = new HashSet<>(client.getAllRooms());
        log.debug("Checking lobby status for client {} - All rooms: {}", client.getSessionId(), rooms);

        rooms.remove("");
        log.debug("Rooms after removing empty: {}", rooms);

        boolean inLobby = rooms.size() == 1 && rooms.contains(LOBBY_ROOM);
        log.debug("Is user in lobby? {}", inLobby);

        return inLobby;
    }

    public LobbyUpdateResponseDto sendEventRoomCreate(RoomCreateResponseDto responseDto, SocketIOClient client) {

        // 비밀방인 경우 로비에 보내지 않는다.
        if(responseDto.getIsPrivate()) return null;

        System.out.println("방 아이디: "+responseDto.getRoomId()+" 방 제목: "+responseDto.getRoomTitle()+" 비밀방 여부: "+ false +" 인원 수: "+ responseDto.getCurrentPlayers().size()+"플레이 중: "+responseDto.getPlaying());

        UpdateData updateData = UpdateData.builder()
                .roomId(responseDto.getRoomId())
                .roomTitle(responseDto.getRoomTitle())
                .isPrivate(responseDto.getIsPrivate())
                .currentPlayerNum(responseDto.getCurrentPlayers().size())
                .playing(responseDto.getPlaying())
                .build();

        return LobbyUpdateResponseDto.builder()
                .type("create")
                .data(updateData)
                .build();
    }

}