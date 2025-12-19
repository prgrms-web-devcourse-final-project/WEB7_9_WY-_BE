package back.kalender.domain.chat.controller;

import back.kalender.domain.chat.dto.response.ChatHistoryResponse;
import back.kalender.domain.chat.dto.response.ChatRoomInfoResponse;
import back.kalender.domain.chat.dto.response.MyChatRoomsResponse;
import back.kalender.domain.chat.dto.response.ParticipantListResponse;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

public interface ChatRoomControllerSpec {

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = """
                    내가 참여 중인 채팅방 목록을 조회합니다.
                    
                    **표시 조건:**
                    - 2명 이상인 채팅방만 표시 (파티장 혼자 있는 채팅방은 제외)
                    - 내가 활성 멤버인 파티의 채팅방만 표시
                    
                    **정렬:**
                    - 마지막 메시지 시간 기준 최신순
                    
                    **용도:**
                    - 채팅 목록 페이지에서 사용
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = MyChatRoomsResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "chatRooms": [
                                        {
                                          "partyId": 1,
                                          "partyName": "지민이 최애 🎤",
                                          "participantCount": 3,
                                          "lastMessage": "안녕하세요! 같이 공연 가요~",
                                          "lastMessageTime": "2024-12-16T14:30:00",
                                          "unreadCount": 0
                                        },
                                        {
                                          "partyId": 2,
                                          "partyName": "뉴진스와 함께 🐰",
                                          "participantCount": 4,
                                          "lastMessage": "입장하셨습니다",
                                          "lastMessageTime": "2024-12-16T10:15:00",
                                          "unreadCount": 0
                                        }
                                      ],
                                      "totalCount": 2
                                    }
                                    """)
                    )
            )
    })
    @GetMapping
    ResponseEntity<MyChatRoomsResponse> getMyChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "채팅방 정보 조회",
            description = """
                    특정 파티의 채팅방 정보를 조회합니다.
                    
                    **반환 정보:**
                    - 파티명, 참여자 수, 최대 인원
                    - 채팅방 활성화 여부
                    - 생성 시간
                    
                    **권한:** 파티 멤버만 조회 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ChatRoomInfoResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "partyId": 1,
                                      "partyName": "지민이 최애 🎤",
                                      "participantCount": 3,
                                      "maxParticipants": 5,
                                      "isActive": true,
                                      "createdAt": "2024-12-15T10:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "8001",
                              "message": "채팅방을 찾을 수 없습니다."
                            }
                            """))
            )
    })
    @GetMapping("/{partyId}")
    ResponseEntity<ChatRoomInfoResponse> getChatRoomInfo(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "채팅방 참여자 목록 조회",
            description = """
                    채팅방의 모든 참여자 정보를 조회합니다.
                    
                    **정렬:** 파티장이 항상 맨 위
                    
                    **권한:** 파티 멤버만 조회 가능
               
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ParticipantListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "partyId": 1,
                                      "participants": [
                                        {
                                          "userId": 1,
                                          "nickname": "파티장님",
                                          "profileImage": "https://example.com/profile1.jpg",
                                          "isLeader": true,
                                          "isOnline": false
                                        },
                                        {
                                          "userId": 2,
                                          "nickname": "팬덤러버",
                                          "profileImage": "https://example.com/profile2.jpg",
                                          "isLeader": false,
                                          "isOnline": false
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            )
    })
    @GetMapping("/{partyId}/participants")
    ResponseEntity<ParticipantListResponse> getParticipants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "채팅 히스토리 조회",
            description = """
                    채팅방의 이전 메시지를 페이징하여 조회합니다.
                    
                    **호출 시점:**
                    1. 채팅방 입장 시 최초 호출 (WebSocket 구독 전)
                    2. 스크롤 업 시 이전 메시지 로드
                    
                    **메시지 타입:**
                    - CHAT: 일반 메시지 (message 필드 있음)
                    - JOIN: 입장 메시지 (message null)
                    - LEAVE: 퇴장 메시지 (message null)
                    - KICK: 강퇴 메시지 (kickedByLeaderId, kickedByLeaderNickname 포함)
                    
                    **페이징:**
                    - 기본 50개씩 조회
                    - 오래된 메시지부터 정렬 (위→아래)
                    - hasNext: true면 더 이전 메시지 존재
                    
                    **권한:** 파티 멤버만 조회 가능
                    
                    **참고:** 입장 전 메시지도 모두 조회 가능 (카카오톡 오픈채팅 방식)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = ChatHistoryResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "partyId": 1,
                                      "messages": [
                                        {
                                          "messageId": 1,
                                          "type": "JOIN",
                                          "senderId": 1,
                                          "senderNickname": "파티장님",
                                          "senderProfileImage": "https://example.com/profile1.jpg",
                                          "message": null,
                                          "timestamp": "2024-12-16T10:00:00",
                                          "kickedByLeaderId": null,
                                          "kickedByLeaderNickname": null
                                        },
                                        {
                                          "messageId": 2,
                                          "type": "CHAT",
                                          "senderId": 1,
                                          "senderNickname": "파티장님",
                                          "senderProfileImage": "https://example.com/profile1.jpg",
                                          "message": "안녕하세요!",
                                          "timestamp": "2024-12-16T10:01:00",
                                          "kickedByLeaderId": null,
                                          "kickedByLeaderNickname": null
                                        },
                                        {
                                          "messageId": 3,
                                          "type": "KICK",
                                          "senderId": 3,
                                          "senderNickname": "문제유저",
                                          "senderProfileImage": null,
                                          "message": null,
                                          "timestamp": "2024-12-16T10:10:00",
                                          "kickedByLeaderId": 1,
                                          "kickedByLeaderNickname": "파티장님"
                                        }
                                      ],
                                      "currentPage": 0,
                                      "totalPages": 1,
                                      "totalMessages": 3,
                                      "hasNext": false
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))
            )
    })
    @GetMapping("/{partyId}/messages")
    ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable Long partyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}