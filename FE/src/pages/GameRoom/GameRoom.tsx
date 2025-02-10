import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import PlayerIcon1 from "../../assets/player-icon/player-icon-1.svg";
import PlayerIcon2 from "../../assets/player-icon/player-icon-2.svg";
import PlayerIcon3 from "../../assets/player-icon/player-icon-3.svg";
import PlayerIcon4 from "../../assets/player-icon/player-icon-4.svg";
import PlayerIcon5 from "../../assets/player-icon/player-icon-5.svg";
import PlayerIcon6 from "../../assets/player-icon/player-icon-1.svg";
import Sample from "../../assets/svg-icon/sample.png";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { IconDefinition } from "@fortawesome/fontawesome-svg-core";
import {
  faBell,
  faGear,
  faUserGroup,
  faDoorOpen,
  faCircleExclamation,
} from "@fortawesome/free-solid-svg-icons";
import ChatBubble from "../GamePlay/components/ChatBuble";
import SystemBubble from "../GamePlay/components/SystemBubble";
import ReadyProfile from "./components/ReadyProfile";
import EmptyProfile from "./components/EmptyProfile";
import ChatForm from "./components/ChatForm";
import { useSocket } from "../../hooks/useSocket";

interface IMessage {
  id: number;
  player: number;
  text: string;
  isMine: boolean; // true면 내가 보낸 메시지, false면 상대방 메시지
}
interface IUser {
  id: number;
  playerNum: number;
  name: string;
  token: string;
  imgNum: number;
}
interface IEmitDone {
  success: boolean;
  errorMessage: string;
  enterId: number;
}

function GameRoom() {
  const { number: roomId } = useParams();
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const password = searchParams.get("pwd");

  const iconArr = [
    PlayerIcon1,
    PlayerIcon2,
    PlayerIcon3,
    PlayerIcon4,
    PlayerIcon5,
    PlayerIcon6,
  ];

  //socket 훅 사용
  const socket = useSocket();

  //아이콘
  const bell: IconDefinition = faBell;
  const gear: IconDefinition = faGear;
  const userGroup: IconDefinition = faUserGroup;
  const doorOpen: IconDefinition = faDoorOpen;
  const circleExclamation: IconDefinition = faCircleExclamation;

  // 현재 접속한 사용자(본인) playerNum 예시 (실제로는 로그인 정보나 URL 파라미터로 받는 등 구현 필요)

  console.log(socket);

  const [roomError, setRoomError] = useState(false);
  const [RoomErrorMessage, setRoomErrorMessage] = useState("");

  //플레이어 본인
  //본인 엔터 아이다와 정보 각각
  const [playerEnterId, setPlayerEnterId] = useState<number>();
  const [playerInfo, setPlayerInfo] = useState<IUser | undefined>({
    id: 999,
    playerNum: 1,
    name: "DummyUser",
    token: "dummy-token-123",
    imgNum: 1,
  });

  //방 참여자 전원
  const [users, setUsers] = useState<IUser[]>([
    {
      id: 999,
      playerNum: 1,
      name: "DummyUser",
      token: "dummy-token-123",
      imgNum: 1,
    },
  ]);

  const player1 = useMemo(
    () => users?.find((user) => user.playerNum === 1),
    [users]
  );
  const player2 = useMemo(
    () => users?.find((user) => user.playerNum === 2),
    [users]
  );
  const player3 = useMemo(
    () => users?.find((user) => user.playerNum === 3),
    [users]
  );
  const player4 = useMemo(
    () => users?.find((user) => user.playerNum === 4),
    [users]
  );
  const player5 = useMemo(
    () => users?.find((user) => user.playerNum === 5),
    [users]
  );
  const player6 = useMemo(
    () => users?.find((user) => user.playerNum === 6),
    [users]
  );

  const [messages, setMessages] = useState<IMessage[]>([]);

  const addMessage = (newMessage: IMessage) => {
    //배열의 마지막 요소에서 서버 시간 비교를 통한 정렬 기능이 필요한가?
    setMessages([...messages, newMessage]);
  };

  const scrollToBottom = () => {
    scrollRef.current?.scrollIntoView({ block: "end" });
  };

  // ----------------------------------------------------
  // 1) 서버에서 보내주는 room:create, room:enter, room:leave 이벤트 수신
  // ----------------------------------------------------
  useEffect(() => {
    if (!socket) return;

    // 방이 생성되었을 때 수신(서버에서 emit('room:create', data)로 보냈다고 가정)

    // 누군가(또는 내가) 방에 입장했을 때 (서버에서 emit('room:enter', data))
    socket.on("room:enter:send", (data: any) => {
      console.log("방 입장 이벤트 수신:", data);
      // 예: { roomId, roomTitle, isPrivate, playing, currentPlayers: [...] }
      if (data.currentPlayers) {
        const newUsers: IUser[] = data.currentPlayers
          .sort((a: any, b: any) => a.enterId - b.enterId)
          .map((player: any, index: number) => ({
            id: player.enterId,
            playerNum: index + 1, // 1부터 시작
            name: player.nickname,
            token: "",
            imgNum: 1,
          }))
          .slice(0, 6);
        setUsers(newUsers);
        setPlayerInfo(newUsers.find((user) => user.id === playerEnterId));
      }
    });

    // 누군가(또는 내가) 방에서 나갔을 때 (서버에서 emit('room:leave', data))
    socket.on("room:leave", (data: any) => {
      console.log("방 퇴장 이벤트 수신:", data);
      // 예:
      // 1) 방에서 나간 사람 정보: { enterId: 2, roomId: 1 }
      // 2) 남아 있는 사람 목록 정보: { currentPlayers: [...] }
      if (data.currentPlayers) {
        const newUsers: IUser[] = data.currentPlayers.map((player: any) => ({
          id: player.enterId,
          playerNum: player.enterId,
          name: player.nickname,
          token: "",
          imgNum: 1,
        }));
        setUsers(newUsers);
      }
    });

    // 클린업
    return () => {
      socket.off("room:create");
      socket.off("room:enter");
      socket.off("room:leave");
    };
  }, [socket]);

  // ----------------------------------------------------
  // 2) 클라이언트에서 서버로 이벤트 emit (방 생성, 입장, 퇴장)
  // ----------------------------------------------------

  const handleEnterRoom = useCallback(() => {
    // 방 입장 요청
    if (!socket) return;
    // roomId는 string일 수 있으니 Number(roomId) 등으로 파싱
    socket.emit(
      "room:enter:emit",
      {
        roomId: Number(roomId),
        password: password,
      },
      ({ success, errorMessage, enterId }: IEmitDone) => {
        if (success) {
          //올바른 방인 경우
          setPlayerEnterId(enterId);
          return;
        } else {
          setRoomError(true);
          setRoomErrorMessage(
            errorMessage || "입장 도중 에러러가 발생했습니다."
          );
          return;
        }
      }
    );
  }, [socket, roomId, playerInfo]);

  const handleLeaveRoom = useCallback(() => {
    // 방 퇴장 요청
    if (!socket || !playerInfo) return;
    socket.emit("room:leave", {
      enterId: playerInfo.id, // 실제로는 playerId로 변경하는 것을 추천
      roomId: Number(roomId),
    });
    // 퇴장 후 메인 페이지로 이동하거나, 다른 페이지로 이동할 수 있음
    navigate("/");
  }, [socket, roomId, playerInfo, navigate]);

  //컴포넌트 마운트시 방에 입장
  //반복 입장하지 않기 위해 useRef 사용
  const didEnterRoomRef = useRef(false);

  useEffect(() => {
    if (!didEnterRoomRef.current && socket && playerInfo && roomId) {
      handleEnterRoom();
      didEnterRoomRef.current = true; // 다시 못 들어가도록
    }
  }, [socket, playerInfo, roomId, handleEnterRoom]);

  useEffect(() => {
    // 새로운 메시지가 추가될 때 스크롤을 아래로 이동
    scrollToBottom();
  }, [messages]);

  console.log(scrollRef);
  return (
    <div className="bg-[#07070a]">
      <div className="background-gradient max-w-[90rem] mx-auto px-4 sm:px-4 md:px-6">
        <div className="hidden lg:flex flex-col justify-between items-center fixed z-20 inset-0 left-[max(0px,calc(50%-45rem))] right-auto w-[21rem] pb-10 pt-6 pl-6 pr-4 bg-black bg-opacity-70 shadow-[0px_0px_16px_rgba(255,255,255,0.25)] border-r-2 border-solid border-r-[rgba(255,255,255,0.35)]">
          <div className="w-full text-base flex justify-center items-center text-[white] bg-[rgb(7,7,10)] px-1.5 py-1 border-2 border-solid border-[rgba(255,255,255,0.35)] rounded-md">
            No. 001 방 제목
          </div>
          <div className="flex flex-col items-center justify-center profile w-52 h-52 border-2 border-solid border-[rgba(255,255,255,0.35)] bg-[#07070a4d]">
            <img
              className="filter brightness-75 w-28 h-28 mb-3"
              src={PlayerIcon1}
            />
            <span className="text-base font-bold justify-center text-[#cccccc] mb-1">
              유저닉네임
            </span>
          </div>
          <button className="w-52 h-14 bg-gradient-to-r from-black via-black to-black rounded-[5px] backdrop-blur-[12.20px] justify-center items-center inline-flex mb-6">
            <div className="w-52 h-14 relative">
              <div className="w-52 h-14 left-0 top-0 absolute opacity-90 bg-black/50 rounded-[5px] shadow-[inset_0px_0px_17px_4px_rgba(255,222,32,0.25)] border-2 border-[#ffc07e]/70" />
              <div className="w-52 h-14 left-0 top-0 absolute flex justify-center items-center text-white text-xl font-normal font-['Inder']">
                게임 시작
              </div>
            </div>
          </button>

          <div className="w-full">
            <div className="config-container w-[3rem] h-[16rem] bg-[#ff3939]/10 rounded-xl flex flex-col justify-between py-4">
              <button>
                <FontAwesomeIcon
                  icon={bell}
                  className="text-white p-1 w-[1.25rem] h-[1.25rem]"
                />
              </button>
              <button>
                <FontAwesomeIcon
                  icon={gear}
                  className="text-white p-1 w-[1.25rem] h-[1.25rem]"
                />
              </button>
              <button>
                <FontAwesomeIcon
                  icon={userGroup}
                  className="text-white p-1 w-[1.25rem] h-[1.25rem]"
                />
              </button>
              <button>
                <FontAwesomeIcon
                  icon={circleExclamation}
                  className="text-white p-1 w-[1.25rem] h-[1.25rem]"
                />
              </button>
              <button
                onClick={() => {
                  handleLeaveRoom();
                  navigate("/game");
                }}
              >
                <FontAwesomeIcon
                  icon={doorOpen}
                  className="text-white p-1 w-[1.25rem] h-[1.25rem]"
                />
              </button>
            </div>
          </div>
        </div>
        <div className="lg:pl-[19.5rem]">
          <div className="max-w-3xl mx-auto xl:max-w-none xl:ml-0 xl:mr-[32rem]">
            <div className="chat-container flex flex-col h-screen overflow-auto">
              {/* 메시지 리스트 영역 */}
              <div className="flex-1 px-5 pt-4">
                {messages.map((msg: IMessage) => {
                  if (msg.player === 10) {
                    return <SystemBubble key={msg.id} message={msg} />;
                  } else {
                    return (
                      <ChatBubble
                        key={msg.id}
                        message={msg}
                        playerName={
                          users.find((user) => user.playerNum === msg.player)
                            ?.name
                        }
                      />
                    );
                  }
                })}
                <div
                  ref={scrollRef}
                  className="chat-input-temp h-[4.5rem] w-full"
                ></div>
                <div className="chat-input fixed h-10 bottom-4 w-[calc(90rem-21rem-33.5rem-2rem)]">
                  <ChatForm playerNum={playerInfo?.playerNum} socket={socket} />
                </div>
              </div>
              <div className="fixed z-20 right-[max(0px,calc(50%-45rem))] w-[33.5rem] py-6 px-3 hidden h-screen bg-black bg-opacity-40 xl:grid grid-cols-3 grid-rows-4 gap-4 shadow-[0px_0px_16px_rgba(255,255,255,0.25)]  border-solid border-l-[rgba(255,255,255,0.35)]">
                <div className="row-start-1 px-2 py-1">
                  {player1 ? (
                    <ReadyProfile
                      nickname={player1.name}
                      playerNum={player1.playerNum}
                      icon={iconArr[player1.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="row-start-1 px-2 py-1">
                  {player2 ? (
                    <ReadyProfile
                      nickname={player2.name}
                      playerNum={player2.playerNum}
                      icon={iconArr[player2.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="row-start-1 px-2 py-1">
                  {player3 ? (
                    <ReadyProfile
                      nickname={player3.name}
                      playerNum={player3.playerNum}
                      icon={iconArr[player3.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="row-start-2 px-2 py-1">
                  {player4 ? (
                    <ReadyProfile
                      nickname={player4.name}
                      playerNum={player4.playerNum}
                      icon={iconArr[player4.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="row-start-2 px-2 py-1">
                  {player5 ? (
                    <ReadyProfile
                      nickname={player5.name}
                      playerNum={player5.playerNum}
                      icon={iconArr[player5.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="row-start-2 px-2 py-1">
                  {player6 ? (
                    <ReadyProfile
                      nickname={player6.name}
                      playerNum={player6.playerNum}
                      icon={iconArr[player6.imgNum]}
                    />
                  ) : (
                    <EmptyProfile />
                  )}
                </div>
                <div className="w-full text-base flex flex-col justify-between items-center row-start-3 row-end-5 col-span-3 text-white px-2 py-1">
                  <div className="w-full text-base flex justify-end items-center text-white px-2 py-1">
                    0/6
                  </div>
                  <div className="w-full h-[80%] text-base justify-center items-center bg-[rgb(7,7,10)] border-2 border-solid border-[#B4B4B4] text-white px-2 py-1">
                    <div>시스템 로그</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default GameRoom;
