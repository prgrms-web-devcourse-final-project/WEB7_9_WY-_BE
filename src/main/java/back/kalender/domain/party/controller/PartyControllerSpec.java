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

    @Operation(
            summary = "파티 목록 조회",
            description = """
                    모집중인 전체 파티 목록을 조회합니다.
                    
                    **Response 특징:**
                    - participationType: null (내 파티 여부와 무관)
                    - applicationId: 내가 신청한 파티인 경우에만 존재
                    - applicationStatus: 내가 신청한 파티인 경우에만 존재
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    - page, size 파라미터로 조절 가능
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 123,
                                          "schedule": {
                                            "scheduleId": 456,
                                            "title": "BTS 콘서트 2025"
                                          },
                                          "leader": {
                                            "leaderId": 789,
                                            "nickname": "파티장"
                                          },
                                          "partyDetail": {
                                            "partyType": "LEAVE",
                                            "partyName": "즐거운 파티",
                                            "departureLocation": "강남역",
                                            "arrivalLocation": "잠실종합운동장",
                                            "transportType": "TAXI",
                                            "maxMembers": 4,
                                            "currentMembers": 2,
                                            "preferredGender": "ANY",
                                            "preferredAge": "TWENTY",
                                            "status": "RECRUITING",
                                            "description": "같이 가요!"
                                          },
                                          "isMyParty": false,
                                          "isApplied": false,
                                          "participationType": null,
                                          "applicationId": null,
                                          "applicationStatus": null
                                        }
                                      ],
                                      "totalElements": 10,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    ))
    })
    @GetMapping
    ResponseEntity<CommonPartyResponse> getParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "스케줄별 파티 목록 조회",
            description = """
                    특정 스케줄에 해당하는 파티 목록을 조회합니다.
                    
                    **필터링:**
                    - partyType: 파티 타입 (LEAVE/RETURN)
                    - transportType: 교통수단 (TAXI/CARPOOL/PUBLIC)
                    
                    **Response 특징:**
                    - participationType: null (내 파티 여부와 무관)
                    - applicationId: 내가 신청한 파티인 경우에만 존재
                    - applicationStatus: 내가 신청한 파티인 경우에만 존재
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 123,
                                          "schedule": {
                                            "scheduleId": 456,
                                            "title": "BTS 콘서트 2025"
                                          },
                                          "leader": {
                                            "leaderId": 789,
                                            "nickname": "파티장"
                                          },
                                          "partyDetail": {
                                            "partyType": "LEAVE",
                                            "partyName": "즐거운 파티",
                                            "departureLocation": "강남역",
                                            "arrivalLocation": "잠실종합운동장",
                                            "transportType": "TAXI",
                                            "maxMembers": 4,
                                            "currentMembers": 2,
                                            "preferredGender": "ANY",
                                            "preferredAge": "TWENTY",
                                            "status": "RECRUITING",
                                            "description": "같이 가요!"
                                          },
                                          "isMyParty": false,
                                          "isApplied": false,
                                          "participationType": null,
                                          "applicationId": null,
                                          "applicationStatus": null
                                        }
                                      ],
                                      "totalElements": 5,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    )
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
    ResponseEntity<CommonPartyResponse> getPartiesBySchedule(
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

    @Operation(
            summary = "내가 만든 파티 목록 조회",
            description = """
                    내가 생성한 파티 목록을 조회합니다.
                    
                    **Response 특징:**
                    - participationType: "CREATED" (고정)
                    - applicationId: null (내가 만든 파티이므로 신청 없음)
                    - applicationStatus: null
                    
                    **조회 대상:**
                    - 내가 파티장인 파티
                    - 상태: RECRUITING, CLOSED (활성 상태만)
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 123,
                                          "schedule": {
                                            "scheduleId": 456,
                                            "title": "BTS 콘서트 2025"
                                          },
                                          "leader": {
                                            "leaderId": 789,
                                            "nickname": "내닉네임"
                                          },
                                          "partyDetail": {
                                            "partyType": "LEAVE",
                                            "partyName": "즐거운 파티",
                                            "departureLocation": "강남역",
                                            "arrivalLocation": "잠실종합운동장",
                                            "transportType": "TAXI",
                                            "maxMembers": 4,
                                            "currentMembers": 3,
                                            "preferredGender": "ANY",
                                            "preferredAge": "TWENTY",
                                            "status": "RECRUITING",
                                            "description": "같이 가요!"
                                          },
                                          "isMyParty": true,
                                          "isApplied": false,
                                          "participationType": "CREATED",
                                          "applicationId": null,
                                          "applicationStatus": null
                                        }
                                      ],
                                      "totalElements": 3,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    ))
    })
    @GetMapping("/user/me/party/created")
    ResponseEntity<CommonPartyResponse> getMyCreatedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "신청중인 파티 목록 조회",
            description = """
                    내가 신청한 파티 중 대기중인 파티만 조회합니다.
                    
                    **Response 특징:**
                    - participationType: "PENDING" (고정)
                    - applicationId: 신청 ID (신청 취소 시 사용)
                    - applicationStatus: "PENDING" (대기중)
                    
                    **조회 대상:**
                    - 신청 상태가 PENDING(대기중)인 파티
                    - 상태: RECRUITING, CLOSED (활성 파티만)
                    
                    **활용:**
                    - 신청 취소 기능에 applicationId 사용
                    - 신청 상태 확인
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    - 신청 시간 기준 최신순
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 123,
                                          "schedule": {
                                            "scheduleId": 456,
                                            "title": "BTS 콘서트 2025"
                                          },
                                          "leader": {
                                            "leaderId": 789,
                                            "nickname": "파티장"
                                          },
                                          "partyDetail": {
                                            "partyType": "LEAVE",
                                            "partyName": "즐거운 파티",
                                            "departureLocation": "강남역",
                                            "arrivalLocation": "잠실종합운동장",
                                            "transportType": "TAXI",
                                            "maxMembers": 4,
                                            "currentMembers": 2,
                                            "preferredGender": "ANY",
                                            "preferredAge": "TWENTY",
                                            "status": "RECRUITING",
                                            "description": "같이 가요!"
                                          },
                                          "isMyParty": false,
                                          "isApplied": true,
                                          "participationType": "PENDING",
                                          "applicationId": 101,
                                          "applicationStatus": "PENDING"
                                        }
                                      ],
                                      "totalElements": 2,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    ))
    })
    @GetMapping("/user/me/party/pending")
    ResponseEntity<CommonPartyResponse> getMyPendingApplications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "참여중인 파티 목록 조회",
            description = """
                    내가 신청하여 승인받은 파티 목록을 조회합니다.
                    
                    **Response 특징:**
                    - participationType: "JOINED" (고정)
                    - applicationId: 신청 ID
                    - applicationStatus: "APPROVED" (승인됨)
                    
                    **조회 대상:**
                    - 신청 상태가 APPROVED(승인됨)인 파티
                    - 상태: RECRUITING, CLOSED (활성 파티만)
                    - 현재 활동 중인 파티
                    
                    **활용:**
                    - 채팅방 입장 가능한 파티 확인
                    - 참여 이력 조회
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    - 승인 시간 기준 최신순
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 124,
                                          "schedule": {
                                            "scheduleId": 457,
                                            "title": "아이유 콘서트 2025"
                                          },
                                          "leader": {
                                            "leaderId": 790,
                                            "nickname": "파티장2"
                                          },
                                          "partyDetail": {
                                            "partyType": "RETURN",
                                            "partyName": "귀가 파티",
                                            "departureLocation": "잠실종합운동장",
                                            "arrivalLocation": "강남역",
                                            "transportType": "SUBWAY",
                                            "maxMembers": 6,
                                            "currentMembers": 4,
                                            "preferredGender": "FEMALE",
                                            "preferredAge": "TWENTY",
                                            "status": "RECRUITING",
                                            "description": "안전하게 귀가해요!"
                                          },
                                          "isMyParty": false,
                                          "isApplied": true,
                                          "participationType": "JOINED",
                                          "applicationId": 102,
                                          "applicationStatus": "APPROVED"
                                        }
                                      ],
                                      "totalElements": 3,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    ))
    })
    @GetMapping("/user/me/party/joined")
    ResponseEntity<CommonPartyResponse> getMyJoinedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "종료된 파티 목록 조회",
            description = """
                    내가 관련된 모든 종료된 파티를 조회합니다.
                    
                    **Response 특징:**
                    - participationType: "CREATED" 또는 "JOINED"
                    - applicationId: 참여한 파티인 경우에만 존재
                    - applicationStatus: "COMPLETED" (종료됨)
                    
                    **포함되는 파티:**
                    - 내가 생성한 종료된 파티 (participationType: CREATED)
                    - 내가 참여한 종료된 파티 (participationType: JOINED)
                    
                    **조회 대상:**
                    - 상태: COMPLETED (종료됨)
                    
                    **페이징:**
                    - 기본 20개씩 조회
                    - 종료 시간 기준 최신순
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CommonPartyResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parties": [
                                        {
                                          "partyId": 125,
                                          "schedule": {
                                            "scheduleId": 458,
                                            "title": "세븐틴 콘서트 2024"
                                          },
                                          "leader": {
                                            "leaderId": 791,
                                            "nickname": "파티장3"
                                          },
                                          "partyDetail": {
                                            "partyType": "LEAVE",
                                            "partyName": "종료된 파티",
                                            "departureLocation": "강남역",
                                            "arrivalLocation": "잠실",
                                            "transportType": "TAXI",
                                            "maxMembers": 4,
                                            "currentMembers": 4,
                                            "preferredGender": "ANY",
                                            "preferredAge": "TWENTY",
                                            "status": "COMPLETED",
                                            "description": "즐거웠습니다!"
                                          },
                                          "isMyParty": false,
                                          "isApplied": true,
                                          "participationType": "JOINED",
                                          "applicationId": 103,
                                          "applicationStatus": "COMPLETED"
                                        }
                                      ],
                                      "totalElements": 5,
                                      "totalPages": 1,
                                      "pageNumber": 0
                                    }
                                    """)
                    ))
    })
    @GetMapping("/user/me/party/completed")
    ResponseEntity<CommonPartyResponse> getMyCompletedParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "파티 모집 마감",
            description = """
                    파티 모집을 마감합니다. 파티장만 마감할 수 있습니다.
                    
                    **마감 후:**
                    - 파티 상태: RECRUITING → CLOSED
                    - 새로운 신청 불가
                    - 채팅방은 계속 사용 가능
                    - 기존 멤버는 유지됨
                    
                    **주의:**
                    - 파티장만 마감 가능
                    - RECRUITING 상태의 파티만 마감 가능
                    - 마감 후 다시 모집 재개 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마감 성공",
                    content = @Content(schema = @Schema(implementation = ClosePartyResponse.class),
                            examples = @ExampleObject(value = """
                                {
                                  "partyId": 1,
                                  "message": "모집이 마감되었습니다."
                                }
                                """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(value = """
                        {
                          "code": "3212",
                          "message": "모집중인 파티가 아닙니다."
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
    @PatchMapping("/{partyId}/close")
    ResponseEntity<ClosePartyResponse> closeParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}