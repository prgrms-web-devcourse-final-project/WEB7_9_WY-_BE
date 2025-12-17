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
 * 실제 로직은 ChatController에서 처리되며, 이 컨트롤러는 Swagger 문서 생성용입니다.
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
            
            ### WebSocket 연결 정보
            - **SEND Destination**: `/app/chat.join/{partyId}`
            - **SUBSCRIBE Destination**: `/topic/room/{partyId}`
            - **Request Body**: 없음 (partyId만 필요)
            
            ### 동작 방식
            1. WebSocket 연결 및 JWT 인증
            2. `/topic/room/{partyId}` 구독
            3. `/app/chat.join/{partyId}`로 입장 메시지 전송
            4. 입장 알림이 모든 참여자에게 브로드캐스트됨
            
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
                            examples = @ExampleObject(
                                    name = "입장 응답",
                                    value = """
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
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "파티 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 파티")
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
            **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 연결 정보
            - **SEND Destination**: `/app/chat.send/{partyId}`
            - **SUBSCRIBE Destination**: `/topic/room/{partyId}`
            - **Request Body**: `SendMessageRequest` (메시지 내용)
            
            ### 동작 방식
            1. `/app/chat.send/{partyId}`로 메시지 전송
            2. 메시지가 모든 참여자에게 브로드캐스트됨
                        
            ---
            
            **권한**: 파티 멤버만 가능  
            **제한**: 메시지 최대 500자
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 전송 성공 - 브로드캐스트 메시지",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatMessageResponse.class),
                            examples = @ExampleObject(
                                    name = "메시지 응답",
                                    value = """
                        {
                          "type": "CHAT",
                          "partyId": 1,
                          "senderId": 5,
                          "senderNickname": "팬덤러버",
                          "senderProfileImage": "https://example.com/profile.jpg",
                          "message": "안녕하세요! 같이 공연 가요~",
                          "timestamp": "2024-12-16T14:30:00"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "메시지가 비어있거나 500자 초과"),
            @ApiResponse(responseCode = "403", description = "파티 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 파티")
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
                            examples = @ExampleObject(
                                    name = "메시지 전송",
                                    value = """
                        {
                          "message": "안녕하세요! 같이 공연 가요~"
                        }
                        """
                            )
                    )
            )
            SendMessageRequest request
    ) {
        throw new UnsupportedOperationException("This is a documentation endpoint. Use WebSocket instead.");
    }

    @Operation(
            summary = "채팅방 나가기 (멤버용)",
            description = """
            **이 API는 문서용입니다. 실제로는 WebSocket을 사용해야 합니다.**
            
            ---
            
            ### WebSocket 연결 정보
            - **SEND Destination**: `/app/chat.leave/{partyId}`
            - **SUBSCRIBE Destination**: `/topic/room/{partyId}`
            - **Request Body**: 없음 (partyId만 필요)
            
            ### 동작 방식
            1. 나가기 버튼 클릭
            2. `/app/chat.leave/{partyId}`로 퇴장 메시지 전송
            3. 퇴장 알림이 모든 참여자에게 브로드캐스트됨
            4. WebSocket 연결 해제
            
            ---
            
            **권한**: 파티 멤버만 가능  
            **제한**: 파티장은 사용 불가
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "퇴장 성공 - 브로드캐스트 메시지",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRoomResponse.class),
                            examples = @ExampleObject(
                                    name = "퇴장 응답",
                                    value = """
                        {
                          "type": "LEAVE",
                          "partyId": 1,
                          "userId": 5,
                          "userNickname": "팬덤러버",
                          "message": "팬덤러버님이 퇴장하셨습니다",
                          "timestamp": "2024-12-16T14:35:00",
                          "participantCount": 3
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "파티장은 나갈 수 없음"),
            @ApiResponse(responseCode = "403", description = "파티 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 파티")
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
            
            ### WebSocket 연결 정보
            - **SEND Destination**: `/app/chat.kick/{partyId}/{targetMemberId}`
            - **SUBSCRIBE Destination**: `/topic/room/{partyId}`
            - **Request Body**: 없음 (URL 파라미터로 대상 지정)
            
            ### 동작 방식
            1. 파티장이 강퇴 버튼 클릭
            2. `/app/chat.kick/{partyId}/{targetMemberId}`로 강퇴 메시지 전송
            3. 강퇴 알림이 모든 참여자에게 브로드캐스트됨
            4. 강퇴된 사용자는 자동으로 연결 해제됨

            ---
            
            **권한**: 파티장만 가능  
            **제한**: 파티장 자신은 강퇴할 수 없음
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "강퇴 성공 - 브로드캐스트 메시지",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = KickMemberResponse.class),
                            examples = @ExampleObject(
                                    name = "강퇴 응답",
                                    value = """
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
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "파티장 자신을 강퇴하려는 경우"),
            @ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 파티 또는 멤버")
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