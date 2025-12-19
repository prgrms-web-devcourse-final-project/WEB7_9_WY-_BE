package back.kalender.domain.party.controller;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

public interface PartyControllerSpec {

    @Operation(
            summary = "파티 생성",
            description = """
                    새로운 파티를 생성합니다.
                    
                    **자동 처리:**
                    - 파티 생성 시 채팅방 자동 생성
                    - 생성자가 자동으로 파티장이 됨
                    - 생성자가 자동으로 첫 번째 멤버로 추가됨
                    
                    **생성 후:**
                    - 파티 상태: RECRUITING (모집 중)
                    - 현재 인원: 1명 (파티장)
                    - 채팅방 활성화: true
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = CreatePartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "partyId": 1,
                                      "leaderId": 1,
                                      "status": "생성 완료"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "001",
                              "message": "잘못된 요청입니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "4001",
                              "message": "일정을 찾을 수 없습니다."
                            }
                            """)))
    })
    @PostMapping
    ResponseEntity<CreatePartyResponse> createParty(
            @Valid @RequestBody CreatePartyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 수정", description = "파티 정보를 수정합니다. 파티장만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdatePartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "partyId": 1,
                                      "leaderId": 1,
                                      "status": "수정 완료"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3413",
                              "message": "현재 인원보다 적게 최대 인원을 설정할 수 없습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3104",
                              "message": "파티장만 파티를 수정할 수 있습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """)))
    })
    @PatchMapping("/{partyId}")
    ResponseEntity<UpdatePartyResponse> updateParty(
            @PathVariable Long partyId,
            @Valid @RequestBody UpdatePartyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "파티 삭제",
            description = """
                    파티를 삭제합니다. 파티장만 삭제할 수 있습니다.
                    
                    **자동 처리:**
                    - 파티 삭제 시 채팅방 자동 종료 (비활성화)
                    - 채팅 메시지는 보존됨 (삭제되지 않음)
                    
                    **주의:**
                    - 파티장만 삭제 가능
                    - 삭제 후 복구 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3105",
                              "message": "파티장만 파티를 삭제할 수 없습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """)))
    })
    @DeleteMapping("/{partyId}")
    ResponseEntity<Void> deleteParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 목록 조회", description = "파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartiesResponse.class)))
    })
    @GetMapping
    ResponseEntity<GetPartiesResponse> getParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "스케줄별 파티 목록 조회",
            description = "특정 스케줄에 해당하는 파티 목록을 조회합니다. 파티 타입과 교통수단으로 필터링할 수 있습니다. 페이징 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartiesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "스케줄을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                    {
                      "code": "4001",
                      "message": "일정을 찾을 수 없습니다."
                    }
                    """))
            )
    })
    @GetMapping("/schedule/{scheduleId}")
    ResponseEntity<GetPartiesResponse> getPartiesBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) PartyType partyType,
            @RequestParam(required = false) TransportType transportType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 참가 신청", description = "파티에 참가 신청을 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신청 성공",
                    content = @Content(schema = @Schema(implementation = ApplyToPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "nickname": "지민이최애",
                                      "age": 25,
                                      "gender": "FEMALE",
                                      "partyName": "즐거운 콘서트 관람"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "본인 파티 신청", value = """
                                    {
                                      "code": "3204",
                                      "message": "본인이 만든 파티에는 신청할 수 없습니다."
                                    }
                                    """),
                            @ExampleObject(name = "이미 신청함", value = """
                                    {
                                      "code": "3202",
                                      "message": "이미 신청한 파티입니다."
                                    }
                                    """),
                            @ExampleObject(name = "이미 멤버임", value = """
                                    {
                                      "code": "3203",
                                      "message": "이미 참여중인 파티입니다."
                                    }
                                    """),
                            @ExampleObject(name = "파티 인원 가득 참", value = """
                                    {
                                      "code": "3205",
                                      "message": "파티 인원이 가득 찼습니다."
                                    }
                                    """),
                            @ExampleObject(name = "모집 중 아님", value = """
                                    {
                                      "code": "3212",
                                      "message": "모집중인 파티가 아닙니다."
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """)))
    })
    @PostMapping("/{partyId}/application/apply")
    ResponseEntity<ApplyToPartyResponse> applyToParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 참가 신청 취소", description = "파티 참가 신청을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3209",
                              "message": "승인된 신청은 취소할 수 없습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3101",
                              "message": "파티에 접근할 권한이 없습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "신청을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3201",
                              "message": "신청 내역을 찾을 수 없습니다."
                            }
                            """)))
    })
    @DeleteMapping("/{partyId}/application/{applicationId}/cancel")
    ResponseEntity<Void> cancelApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "파티 신청 승인",
            description = """
                    파티 신청을 승인합니다. 파티장만 승인할 수 있습니다.
                    
                    **승인 후:**
                    - 신청자가 파티 멤버로 추가됨
                    - 채팅방 입장 가능해짐
                    - 현재 인원 +1
                    - 인원이 가득 차면 파티 상태 자동 변경 (RECRUITING → CLOSED)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공",
                    content = @Content(schema = @Schema(implementation = AcceptApplicationResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "applicantId": 2,
                                      "partyName": "즐거운 콘서트 관람",
                                      "chatRoomId": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = {
                            @ExampleObject(name = "이미 처리된 신청", value = """
                                    {
                                      "code": "3206",
                                      "message": "이미 처리된 신청입니다."
                                    }
                                    """),
                            @ExampleObject(name = "파티 인원 가득 참", value = """
                                    {
                                      "code": "3205",
                                      "message": "파티 인원이 가득 찼습니다."
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3102",
                              "message": "파티장만 이 작업을 수행할 수 있습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "파티 없음", value = """
                                    {
                                      "code": "3001",
                                      "message": "파티를 찾을 수 없습니다."
                                    }
                                    """),
                            @ExampleObject(name = "신청 없음", value = """
                                    {
                                      "code": "3201",
                                      "message": "신청 내역을 찾을 수 없습니다."
                                    }
                                    """)
                    }))
    })
    @PatchMapping("/{partyId}/application/{applicationId}/accept")
    ResponseEntity<AcceptApplicationResponse> acceptApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 신청 거절", description = "파티 신청을 거절합니다. 파티장만 거절할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거절 성공",
                    content = @Content(schema = @Schema(implementation = RejectApplicationResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "applicantId": 2,
                                      "partyName": "즐거운 콘서트 관람",
                                      "chatRoomId": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3206",
                              "message": "이미 처리된 신청입니다."
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3102",
                              "message": "파티장만 이 작업을 수행할 수 있습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "파티 없음", value = """
                                    {
                                      "code": "3001",
                                      "message": "파티를 찾을 수 없습니다."
                                    }
                                    """),
                            @ExampleObject(name = "신청 없음", value = """
                                    {
                                      "code": "3201",
                                      "message": "신청 내역을 찾을 수 없습니다."
                                    }
                                    """)
                    }))
    })
    @PatchMapping("/{partyId}/application/{applicationId}/reject")
    ResponseEntity<RejectApplicationResponse> rejectApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "파티 신청자 목록 조회", description = "파티에 신청한 사람들의 목록을 조회합니다. 파티장만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetApplicantsResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3102",
                              "message": "파티장만 이 작업을 수행할 수 있습니다."
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """)))
    })
    @GetMapping("/{partyId}/application/applicants")
    ResponseEntity<GetApplicantsResponse> getApplicants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "파티 멤버 목록 조회",
            description = """
                    파티에 참여 중인 멤버 목록을 조회합니다.
                    
                    **참고:**
                    - 채팅방 참여자 목록은 별도 API 사용 (GET /api/v1/chat/rooms/{partyId}/participants)
                    - 이 API는 파티 멤버 정보만 조회
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartyMembersResponse.class))),
            @ApiResponse(responseCode = "404", description = "파티를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3001",
                              "message": "파티를 찾을 수 없습니다."
                            }
                            """)))
    })
    @GetMapping("/{partyId}/members")
    ResponseEntity<GetPartyMembersResponse> getPartyMembers(
            @PathVariable Long partyId
    );

    @Operation(summary = "신청한 파티 목록 조회", description = "내가 신청한 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyApplicationsResponse.class)))
    })
    @GetMapping("/user/me/party/application")
    ResponseEntity<GetMyApplicationsResponse> getMyApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "생성한 파티 목록 조회", description = "내가 생성한 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyCreatedPartiesResponse.class)))
    })
    @GetMapping("/user/me/party/created")
    ResponseEntity<GetMyCreatedPartiesResponse> getMyCreatedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "종료된 파티 목록 조회",
            description = """
                내가 관련된 모든 종료된 파티를 조회합니다.
                
                **포함되는 파티:**
                - 내가 생성한 종료된 파티 (participationType: CREATED)
                - 내가 참여한 종료된 파티 (participationType: JOINED)
                
                **정렬 기준:**
                - 종료 시간 기준 최신순
                
                **페이징:**
                - 기본 20개씩 조회
                - page, size 파라미터로 조절 가능
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GetCompletedPartiesResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                  "parties": [
                                    {
                                      "partyId": 1,
                                      "participationType": "CREATED",
                                      "event": {
                                        "eventId": 1,
                                        "title": "BTS 콘서트 2025",
                                        "location": "잠실종합운동장",
                                        "scheduleTime": "2025-12-31T19:00:00"
                                      },
                                      "partyDetail": {
                                        "partyName": "즐거운 BTS 콘서트",
                                        "partyType": "LEAVE",
                                        "departureLocation": "강남역",
                                        "arrivalLocation": "잠실종합운동장",
                                        "maxMembers": 4,
                                        "currentMembers": 4
                                      },
                                      "leader": {
                                        "leaderId": 1,
                                        "nickname": "리더유저1",
                                        "profileImage": "https://example.com/profile.jpg"
                                      },
                                      "status": "COMPLETED",
                                      "completedAt": "2025-12-16T20:00:00",
                                      "createdAt": "2025-12-15T10:00:00"
                                    },
                                    {
                                      "partyId": 2,
                                      "participationType": "JOINED",
                                      "event": {
                                        "eventId": 2,
                                        "title": "블랙핑크 월드투어",
                                        "location": "고척스카이돔",
                                        "scheduleTime": "2025-11-15T20:00:00"
                                      },
                                      "partyDetail": {
                                        "partyName": "블핑 콘서트 같이 가요",
                                        "partyType": "LEAVE",
                                        "departureLocation": "신림역",
                                        "arrivalLocation": "고척스카이돔",
                                        "maxMembers": 5,
                                        "currentMembers": 3
                                      },
                                      "leader": {
                                        "leaderId": 2,
                                        "nickname": "리더유저2",
                                        "profileImage": "https://example.com/profile2.jpg"
                                      },
                                      "status": "COMPLETED",
                                      "completedAt": "2025-11-16T23:00:00",
                                      "createdAt": "2025-11-10T15:00:00"
                                    }
                                  ],
                                  "totalElements": 5,
                                  "totalPages": 1,
                                  "pageNumber": 0
                                }
                                """)
                    )
            )
    })
    @GetMapping("/user/me/party/completed")
    ResponseEntity<GetCompletedPartiesResponse> getMyCompletedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}