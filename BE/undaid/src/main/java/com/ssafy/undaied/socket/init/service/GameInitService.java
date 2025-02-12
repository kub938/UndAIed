package com.ssafy.undaied.socket.init.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.undaied.socket.common.exception.SocketErrorCode;
import com.ssafy.undaied.socket.common.exception.SocketException;
import com.ssafy.undaied.socket.init.dto.response.GameInfoResponseDto;
import com.ssafy.undaied.socket.init.dto.response.PlayerInfoDto;
import com.ssafy.undaied.socket.room.dto.Room;
import com.ssafy.undaied.socket.room.dto.RoomUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ssafy.undaied.socket.common.constant.SocketRoom.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameInitService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final SocketIOServer socketIOServer;
    private final ObjectMapper objectMapper;
    private static final String ROOM_LIST = "rooms:";

    private static final long EXPIRE_TIME = 7200;
    private static final int REQUIRED_PLAYERS = 6;
    private static final int TOTAL_NUMBERS = 8;

    // AI 후보 리스트
    private static final Map<String, String> AI_POOL = new HashMap<>() {{
        put("ai1", "deepseek");
        put("ai2", "gemini");
        put("ai3", "chatgpt");
    }};

//     테스트 데이터. 나중에 지우려고 함.
//@PostConstruct
//public void initTestData() {
//    int testRoomId = 456;
//    String roomKey = ROOM_LIST + testRoomId;
//
//    // 기존 데이터 삭제
//    redisTemplate.delete(roomKey);
//
//    // 테스트용 RoomUser 목록 생성
//    List<RoomUser> players = new ArrayList<>();
//    players.add(RoomUser.builder()
//            .userId(11)
//            .enterId(0)
//            .isHost(true)
//            .nickname("cchh6462")
//            .profileImage(1)
//            .build());
//
//    players.add(RoomUser.builder()
//            .userId(12)
//            .enterId(1)
//            .isHost(false)
//            .nickname("TestUser2")
//            .profileImage(2)
//            .build());
//
//    players.add(RoomUser.builder()
//            .userId(13)
//            .enterId(2)
//            .isHost(false)
//            .nickname("TestUser3")
//            .profileImage(3)
//            .build());
//
//    players.add(RoomUser.builder()
//            .userId(14)
//            .enterId(3)
//            .isHost(false)
//            .nickname("TestUser4")
//            .profileImage(4)
//            .build());
//
//    players.add(RoomUser.builder()
//            .userId(15)
//            .enterId(4)
//            .isHost(false)
//            .nickname("TestUser5")
//            .profileImage(5)
//            .build());
//
//    players.add(RoomUser.builder()
//            .userId(16)
//            .enterId(5)
//            .isHost(false)
//            .nickname("TestUser6")
//            .profileImage(6)
//            .build());
//
//    // Room 객체 생성 및 저장
//    Room room = Room.builder()
//            .roomId((long) testRoomId)
//            .roomTitle("Test Game Room")
//            .isPrivate(false)
//            .roomPassword(null)
//            .playing(false)
//            .currentPlayers(players)
//            .build();
//
//    jsonRedisTemplate.opsForValue().set(roomKey, room);
//
//    // 소켓 room에도 join시키기
//    socketIOServer.getAllClients().forEach(client -> {
//        Integer userId = client.get("userId");
//        if (userId != null && userId == 11) {  // 방장
//            client.joinRoom(roomKey);
//            log.info("Host joined room - userId: {}, roomKey: {}", userId, roomKey);
//        }
//    });
//
//    log.info("Test data initialized - roomId: {}, players: {}", testRoomId, players.stream()
//            .map(user -> String.format("%s(%d)", user.getNickname(), user.getUserId()))
//            .collect(Collectors.joining(", ")));
//
//    log.info("\n=== Postman 테스트 가이드 ===\n" +
//            "1. Socket.IO 연결 설정:\n" +
//            "   URL: ws://localhost:8080\n" +
//            "   Auth:\n" +
//            "   {\n" +
//            "     \"userId\": 11,\n" +
//            "     \"nickname\": \"cchh6462\"\n" +
//            "   }\n\n" +
//            "2. 게임 시작 테스트:\n" +
//            "   이벤트명: game:init\n" +
//            "   데이터 필요 없음 (빈 데이터로 emit)\n" +
//            "=========================");
//
//    // 테스트 데이터 확인용 로그
//    Object savedRoom = jsonRedisTemplate.opsForValue().get(roomKey);
//    log.info("Saved room data: {}", savedRoom);
//}

    public GameInfoResponseDto startGame(SocketIOClient client, int roomId) throws SocketException {
        Integer userId = validateAuthentication(client);
        Room room = getRoomInfo(roomId);

        if (!isHost(userId, room)) {
            throw new SocketException(SocketErrorCode.NOT_HOST);
        }

        List<Integer> players = room.getCurrentPlayers().stream()
                .map(RoomUser::getUserId)
                .collect(Collectors.toList());

        validatePlayers(players);

        int gameId = initGame();
        saveGameRoomMapping(gameId, roomId);

        // ✅ AI 2개 선택 후 저장
        List<String> selectedAIs = selectTwoAIs();
        savePlayersAndAssignNumbers(gameId, players, room, selectedAIs);

        handleSocketConnections(gameId, roomId);
        broadcastGameInit(gameId);

        log.info("Game successfully initialized - gameId: {}, roomKey: {}, players: {}",
                gameId,
                ROOM_LIST + roomId,
                room.getCurrentPlayers().stream()
                        .map(user -> String.format("%s(%d)", user.getNickname(), user.getUserId()))
                        .collect(Collectors.joining(", "))
        );

        return createGameInfoResponse(gameId);
    }

    // AI 3개 중 2개를 랜덤 선택하는 메서드
    private List<String> selectTwoAIs() {
        List<String> shuffledAIs = new ArrayList<>(AI_POOL.keySet()); // AI key(ai1, ai2, ai3) 리스트 가져오기
        Collections.shuffle(shuffledAIs); // 랜덤 섞기
        return shuffledAIs.subList(0, 2); // 앞에서 2개 선택
    }

    private Integer validateAuthentication(SocketIOClient client) throws SocketException {
        Integer userId = client.get("userId");
        if (userId == null) {
            throw new SocketException(SocketErrorCode.SOCKET_AUTHENTICATION_FAILED);
        }
        return userId;
    }

    private boolean isHost(Integer userId, Room room) {
        return room.getCurrentPlayers().stream()
                .anyMatch(player -> player.getUserId().equals(userId) && Boolean.TRUE.equals(player.getIsHost()));
    }

    private Room getRoomInfo(int roomId) throws SocketException {
        String roomKey = ROOM_LIST + roomId;
        Object roomObj = jsonRedisTemplate.opsForValue().get(roomKey);

        if (roomObj == null) {
            log.warn("Room not found in Redis - roomId: {}", roomId);
            throw new SocketException(SocketErrorCode.ROOM_NOT_FOUND);
        }

        Room room = objectMapper.convertValue(roomObj, Room.class);
        if (room.getCurrentPlayers() == null || room.getCurrentPlayers().isEmpty()) {
            log.warn("No players in room - roomId: {}", roomId);
            throw new SocketException(SocketErrorCode.INVALID_PLAYER_COUNT);
        }

        return room;
    }

    private void validatePlayers(List<Integer> players) throws SocketException {
        if (players.size() != REQUIRED_PLAYERS) {
            throw new SocketException(SocketErrorCode.INVALID_PLAYER_COUNT);
        }
    }

    private int initGame() {
        String gameCounterKey = GAME_KEY_PREFIX + "counter";
        Long gameId = redisTemplate.opsForValue().increment(gameCounterKey);
        String gameKey = GAME_KEY_PREFIX + gameId;

        Map<String, Object> gameData = new HashMap<>();
        gameData.put("status", "PROCESS");
        gameData.put("startedAt", LocalDateTime.now().toString());
        gameData.put("endedAt", "00:00");
        gameData.put("playtime", "00:00");
        gameData.put("humanWin", false);

        jsonRedisTemplate.opsForHash().putAll(gameKey, gameData);
        redisTemplate.expire(gameKey, EXPIRE_TIME, TimeUnit.SECONDS);

        return gameId.intValue();
    }

    private void saveGameRoomMapping(int gameId, int roomId) {
        String mappingKey = GAME_KEY_PREFIX + gameId + ":roomId";
        redisTemplate.opsForValue().set(mappingKey, String.valueOf(roomId));
        redisTemplate.expire(mappingKey, EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private void handleSocketConnections(int gameId, int roomId) {
        String roomKey = ROOM_LIST + roomId;
        Object roomObj = jsonRedisTemplate.opsForValue().get(roomKey);

        if (roomObj == null) {
            log.error("Room not found in Redis - roomId: {}", roomId);
            return;
        }

        Room room = objectMapper.convertValue(roomObj, Room.class);

        if (room.getCurrentPlayers() != null && !room.getCurrentPlayers().isEmpty()) {
            List<Integer> roomUserIds = room.getCurrentPlayers().stream()
                    .map(RoomUser::getUserId)
                    .collect(Collectors.toList());

            socketIOServer.getAllClients().forEach(client -> {
                Integer clientUserId = client.get("userId");
                if (clientUserId != null && roomUserIds.contains(clientUserId)) {
                    client.set("gameId", gameId);
                    client.joinRoom(GAME_KEY_PREFIX + gameId);
                    client.leaveRoom(ROOM_LIST + roomId);
                    log.info("Player joined game room - userId: {}, gameId: {}, nickname: {}",
                            clientUserId,
                            gameId,
                            room.getCurrentPlayers().stream()
                                    .filter(user -> user.getUserId().equals(clientUserId))
                                    .map(RoomUser::getNickname)
                                    .findFirst()
                                    .orElse("Unknown")
                    );
                }
            });
        } else {
            log.warn("No players found in room - roomId: {}", roomId);
        }
    }

    private void savePlayersAndAssignNumbers(int gameId, List<Integer> players, Room room, List<String> selectedAIs) {
        List<Integer> availableNumbers = IntStream.rangeClosed(1, TOTAL_NUMBERS)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(availableNumbers);

        // Room 정보에서 닉네임 정보 가져오기
        Map<Integer, String> userNicknames = room.getCurrentPlayers().stream()
                .collect(Collectors.toMap(
                        RoomUser::getUserId,
                        RoomUser::getNickname
                ));

        String mappingKey = GAME_KEY_PREFIX + gameId + ":number_mapping";
        String playersKey = GAME_KEY_PREFIX + gameId + ":players";
        String statusKey = GAME_KEY_PREFIX + gameId + ":player_status";
        String userNicknameKey = GAME_KEY_PREFIX + gameId + ":user_nicknames";
        String numberNicknameKey = GAME_KEY_PREFIX + gameId + ":number_nicknames";

        // 실제 플레이어 할당
        for (int i = 0; i < REQUIRED_PLAYERS; i++) {
            Integer userId = players.get(i);
            Integer assignedNumber = availableNumbers.remove(0);

            redisTemplate.opsForHash().put(mappingKey, userId.toString(), assignedNumber.toString());
            redisTemplate.opsForSet().add(playersKey, userId.toString());

            savePlayerStatus(statusKey, assignedNumber.toString(), false, false, true);
            String nickname = userNicknames.get(userId);
            redisTemplate.opsForHash().put(userNicknameKey, userId.toString(), nickname);
        }


        // AI 역할 매핑 후 랜덤 선택
        List<String> aiRoles = new ArrayList<>(List.of("ai1", "ai2", "ai3"));
        Collections.shuffle(aiRoles);
        String selectedAI1 = aiRoles.get(0);
        String selectedAI2 = aiRoles.get(1);

        // AI를 실제 플레이어 번호에 매핑
        String ai1Number = availableNumbers.remove(0).toString();
        String ai2Number = availableNumbers.remove(0).toString();

        // ✅ 기존 방식 수정: "ai1", "ai2"를 강제하지 않고, 선택된 AI ID를 그대로 저장
        redisTemplate.opsForHash().put(mappingKey, selectedAI1, ai1Number);
        redisTemplate.opsForHash().put(mappingKey, selectedAI2, ai2Number);

        String aiKey = GAME_KEY_PREFIX + gameId + ":ai_numbers";
        redisTemplate.opsForSet().add(aiKey, ai1Number, ai2Number);

        savePlayerStatus(statusKey, ai1Number, false, false, true);
        savePlayerStatus(statusKey, ai2Number, false, false, true);

        redisTemplate.opsForHash().put(userNicknameKey, "ai1", "AI-1");
        redisTemplate.opsForHash().put(userNicknameKey, "ai2", "AI-2");

        createNumberNicknames().forEach((number, nickname) ->
                redisTemplate.opsForHash().put(numberNicknameKey, number.toString(), nickname));

        Arrays.asList(mappingKey, playersKey, aiKey, userNicknameKey, numberNicknameKey, statusKey)
                .forEach(key -> redisTemplate.expire(key, EXPIRE_TIME, TimeUnit.SECONDS));
    }

    private void savePlayerStatus(String statusKey, String number, boolean isDied, boolean isInfected, boolean isInGame) {
        Map<String, String> status = new HashMap<>();
        status.put("number", number);
        status.put("isDied", String.valueOf(isDied));
        status.put("isInfected", String.valueOf(isInfected));
        status.put("isInGame", String.valueOf(isInGame));
        redisTemplate.opsForHash().put(statusKey, number, status.toString());

        String savedStatus = redisTemplate.opsForHash().get(statusKey, number).toString();
        log.debug("Saved player status - key: {}, number: {}, status: {}", statusKey, number, savedStatus);
    }

    private void broadcastGameInit(int gameId) {
        GameInfoResponseDto responseDto = createGameInfoResponse(gameId);
        socketIOServer.getRoomOperations(GAME_KEY_PREFIX + gameId)
                .sendEvent("game:init", responseDto);
    }

    private Map<Integer, String> createNumberNicknames() {
        return IntStream.rangeClosed(1, TOTAL_NUMBERS)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> "익명" + i
                ));
    }

    public void updatePlayerStatus(int gameId, int number, boolean isDied, boolean isInfected, boolean isInGame) {
        savePlayerStatus(GAME_KEY_PREFIX + gameId + ":player_status",
                String.valueOf(number), isDied, isInfected, isInGame);
    }

    public GameInfoResponseDto createGameInfoResponse(int gameId) {
        String statusKey = GAME_KEY_PREFIX + gameId + ":player_status";
        Map<Object, Object> allStatus = redisTemplate.opsForHash().entries(statusKey);

        List<PlayerInfoDto> players = allStatus.entrySet().stream()
                .map(entry -> {
                    String status = entry.getValue().toString();
                    return PlayerInfoDto.builder()
                            .number(Integer.parseInt(entry.getKey().toString()))
                            .isDied(status.contains("isDied=true"))
                            .isInfected(status.contains("isInfected=true"))
                            .isInGame(status.contains("isInGame=true"))
                            .build();
                })
                .sorted(Comparator.comparingInt(PlayerInfoDto::getNumber))
                .collect(Collectors.toList());

        return GameInfoResponseDto.builder()
                .players(players)
                .build();
    }
}