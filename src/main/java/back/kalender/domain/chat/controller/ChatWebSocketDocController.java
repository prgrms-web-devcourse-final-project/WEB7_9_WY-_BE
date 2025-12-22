package back.kalender.domain.chat.controller;

import back.kalender.domain.chat.dto.request.SendMessageRequest;
import back.kalender.domain.chat.dto.response.ChatMessageResponse;
import back.kalender.domain.chat.dto.response.KickMemberResponse;
import back.kalender.domain.chat.dto.response.LeaveRoomResponse;
import back.kalender.domain.chat.dto.response.RoomJoinedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket 채팅 API 문서화 전용 컨트롤러
 *
 * 이 API는 문서 생성용이며, 실제로는 WebSocket을 사용해야 합니다.
 *
 * 실제 WebSocket 엔드포인트:
 * - 연결: ws://localhost:8080/ws-chat (SockJS)
 * - 인증: CONNECT 시 Authorization 헤더에 JWT Bearer 토큰
 */
@Tag(name = "Chat WebSocket", description = "파티 단체 채팅 WebSocket API (문서용)")
@RestController
@RequestMapping("/api/v1/docs/chat/ws")
public class ChatWebSocketDocController {

    @Operation(
            summary = "채팅방 입장",
            description = """
            **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 정보
            - **SEND**: `/app/chat.join/{partyId}`
            - **SUBSCRIBE**: `/topic/room/{partyId}`
            - **인증**: JWT Bearer Token (CONNECT 시 Authorization 헤더)
            
            ### 동작 흐름
            1. WebSocket 연결 (`/ws-chat`)
            2. `/topic/room/{partyId}` 구독
            3. `/app/chat.join/{partyId}` 전송 (body 없음)
            4. 입장 알림이 모든 참여자에게 브로드캐스트
            
            ### 브로드캐스트 메시지
            - type: "JOIN"
            - 입장한 사용자 정보 포함
            - 현재 참여자 수 포함
            
            ---
            
            **권한**: 파티 멤버만 가능
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "입장 성공 - 브로드캐스트 메시지",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RoomJoinedResponse.class),
                            examples = @ExampleObject(value = """
                        {
                          "type": "JOIN",
                          "partyId": 1,
                          "userId": 5,
                          "userNickname": "팬덤러버",
                          "userProfileImage": "https://example.com/profile.jpg",
                          "message": "팬덤러버님이 입장하셨습니다",
                          "timestamp": "2024-12-16T14:30:00",
                          "participantCount": 4
                        }
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "파티 멤버가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 파티",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """))
            )
    })
    @PostMapping("/join/{partyId}")
    public RoomJoinedResponse joinRoomDoc(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @PathVariable Long partyId
    ) {
        throw new UnsupportedOperationException("This is a documentation endpoint. Use WebSocket instead.");
    }

    @Operation(
            summary = "채팅 메시지 전송",
            description = """
            ⚠️ **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 정보
            - **SEND**: `/app/chat.send/{partyId}`
            - **SUBSCRIBE**: `/topic/room/{partyId}`
            - **Request Body**: `{"message": "메시지 내용"}`
            
            ### 동작 흐름
            1. `/app/chat.send/{partyId}`로 메시지 전송
            2. 서버에서 DB 저장
            3. 모든 참여자에게 브로드캐스트
            
            ### 제한사항
            - 메시지 최대 500자
            - 빈 메시지 불가
            
            ---
            
            **권한**: 파티 멤버만 가능
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 전송 성공 - 브로드캐스트",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageResponse.class),
                            examples = @ExampleObject(value = """
                        {
                          "type": "CHAT",
                          "partyId": 1,
                          "senderId": 5,
                          "senderNickname": "팬덤러버",
                          "senderProfileImage": "https://example.com/profile.jpg",
                          "message": "안녕하세요! 같이 공연 가요~",
                          "timestamp": "2024-12-16T14:30:00"
                        }
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "빈 메시지", value = """
                                    {
                                      "code": "8101",
                                      "message": "메시지 내용은 필수입니다."
                                    }
                                    """),
                            @ExampleObject(name = "메시지 초과", value = """
                                    {
                                      "code": "8102",
                                      "message": "메시지는 500자를 초과할 수 없습니다."
                                    }
                                    """)
                    })
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "파티 멤버가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            )
    })
    @PostMapping("/send/{partyId}")
    public ChatMessageResponse sendMessageDoc(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @PathVariable Long partyId,

            @RequestBody(
                    description = "전송할 메시지 내용",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SendMessageRequest.class),
                            examples = @ExampleObject(value = """
                        {
                          "message": "안녕하세요! 같이 공연 가요~"
                        }
                        """)
                    )
            )
            SendMessageRequest request
    ) {
        throw new UnsupportedOperationException("This is a documentation endpoint. Use WebSocket instead.");
    }

    @Operation(
            summary = "채팅방 나가기",
            description = """
            **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 정보
            - **SEND**: `/app/chat.leave/{partyId}`
            - **SUBSCRIBE**: `/topic/room/{partyId}`
            
            ### 동작 흐름
            1. 나가기 버튼 클릭
            2. `/app/chat.leave/{partyId}` 전송
            3. 퇴장 알림 브로드캐스트
            4. WebSocket 연결 해제
            
            ### 제한사항
            - 파티장은 나갈 수 없음
            - 나가면 다시 입장 불가 (파티 멤버에서 제외)
            
            ---
            
            **권한**: 파티 일반 멤버만 가능 (파티장 불가)
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "퇴장 성공 - 브로드캐스트",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRoomResponse.class),
                            examples = @ExampleObject(value = """
                        {
                          "type": "LEAVE",
                          "partyId": 1,
                          "userId": 5,
                          "userNickname": "팬덤러버",
                          "message": "팬덤러버님이 퇴장하셨습니다",
                          "timestamp": "2024-12-16T14:35:00",
                          "participantCount": 3
                        }
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "파티장은 나갈 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "8201",
                              "message": "파티장은 채팅방을 나갈 수 없습니다."
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "파티 멤버가 아님",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            )
    })
    @PostMapping("/leave/{partyId}")
    public LeaveRoomResponse leaveRoomDoc(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @PathVariable Long partyId
    ) {
        throw new UnsupportedOperationException("This is a documentation endpoint. Use WebSocket instead.");
    }

    @Operation(
            summary = "멤버 강퇴 (파티장 전용)",
            description = """
            **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 정보
            - **SEND**: `/app/chat.kick/{partyId}/{targetMemberId}`
            - **SUBSCRIBE**: `/topic/room/{partyId}`
            
            ### 동작 흐름
            1. 파티장이 강퇴 버튼 클릭
            2. `/app/chat.kick/{partyId}/{targetMemberId}` 전송
            3. 강퇴 알림 브로드캐스트
            4. 강퇴된 사용자 자동 연결 해제
            
            ### 제한사항
            - 파티장만 사용 가능
            - 파티장 자신은 강퇴 불가
            
            ---
            
            **권한**: 파티장만 가능
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "강퇴 성공 - 브로드캐스트",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KickMemberResponse.class),
                            examples = @ExampleObject(value = """
                        {
                          "type": "KICK",
                          "partyId": 1,
                          "kickedMemberId": 3,
                          "kickedMemberNickname": "문제유저",
                          "kickedByLeaderId": 1,
                          "kickedByLeaderNickname": "파티장님",
                          "message": "문제유저님이 강퇴되었습니다",
                          "timestamp": "2024-12-16T14:40:00",
                          "participantCount": 3
                        }
                        """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "8202",
                              "message": "자기 자신은 강퇴할 수 없습니다."
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "파티장이 아님",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "8203",
                              "message": "파티장만 멤버를 강퇴할 수 있습니다."
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "대상을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "8204",
                              "message": "파티 멤버가 아닙니다."
                            }
                            """))
            )
    })
    @PostMapping("/kick/{partyId}/{targetMemberId}")
    public KickMemberResponse kickMemberDoc(
            @Parameter(description = "파티 ID", required = true, example = "1")
            @PathVariable Long partyId,

            @Parameter(description = "강퇴할 멤버 ID", required = true, example = "3")
            @PathVariable Long targetMemberId
    ) {
        throw new UnsupportedOperationException("This is a documentation endpoint. Use WebSocket instead.");
    }
}