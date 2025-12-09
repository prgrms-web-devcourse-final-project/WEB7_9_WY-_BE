package back.kalender.domain.party.controller;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/party")
@Validated
@Tag(name = "PartyController", description = "파티 API")
public class PartyController {

    @PostMapping
    @Operation(summary = "파티 생성", description = "새로운 파티를 생성합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = CreatePartyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "최대 인원 범위 초과",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "INVALID_MAX_MEMBERS",
                                                    "status": "400",
                                                    "message": "최대 인원은 2명 이상 10명 이하여야 합니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "출발 시간 과거",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "INVALID_DEPARTURE_TIME",
                                                    "status": "400",
                                                    "message": "출발 시간은 현재 시간 이후여야 합니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "일정을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "SCHEDULE_NOT_FOUND",
                                            "status": "404",
                                            "message": "일정을 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<CreatePartyResponse> createParty(@Valid @RequestBody CreatePartyRequest request) {
        return ResponseEntity.ok().body(new CreatePartyResponse(1L, 2L, "test"));
    }

    @PutMapping()
    @Operation(summary = "파티 수정", description = "파티 정보를 수정합니다. 파티장만 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdatePartyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "CANNOT_REDUCE_MAX_MEMBERS",
                                            "status": "400",
                                            "message": "현재 인원보다 적게 최대 인원을 설정할 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "CANNOT_MODIFY_PARTY_NOT_LEADER",
                                            "status": "403",
                                            "message": "파티장만 파티를 수정할 수 있습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "파티를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "PARTY_NOT_FOUND",
                                            "status": "404",
                                            "message": "파티를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<UpdatePartyResponse> modifyParty(@Valid @RequestBody UpdatePartyRequest request) {
        return ResponseEntity.ok().body(new UpdatePartyResponse(1L, 1L, "수정 완료"));
    }

    @DeleteMapping("/{partyId}")
    @Operation(summary = "파티 삭제", description = "파티를 삭제합니다. 파티장만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "CANNOT_DELETE_PARTY_NOT_LEADER",
                                            "status": "403",
                                            "message": "파티장만 파티를 삭제할 수 있습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "파티를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "PARTY_NOT_FOUND",
                                            "status": "404",
                                            "message": "파티를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<Void> deleteParty(@PathVariable("partyId") Long partyId) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "파티 목록 조회", description = "파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartiesResponse.class))
            )
    })
    public ResponseEntity<GetPartiesResponse> getParties(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        List<GetPartiesResponse.PartyItem> list = List.of(
                new GetPartiesResponse.PartyItem(
                        1L,
                        new GetPartiesResponse.Leader(
                                100L,
                                "지민이 최애",
                                23,
                                Gender.FEMALE,
                                "https://example.com/profile.jpg"
                        ),
                        new GetPartiesResponse.Event(
                                123L,
                                "BTS WORLD TOUR 2025",
                                "잠실종합운동장"
                        ),
                        new GetPartiesResponse.PartyInfo(
                                PartyType.LEAVE,
                                "지민이 최애",
                                "강남역",
                                "잠실종합운동장",
                                TransportType.TAXI,
                                4,
                                2,
                                "같이 즐겁게 콘서트 가요!",
                                PartyStatus.RECRUITING
                        ),
                        false,
                        false
                )
        );

        return ResponseEntity.ok(new GetPartiesResponse(list, 1, 1, 0));
    }

    @PostMapping("/{partyId}/application/apply")
    @Operation(summary = "파티 참가 신청", description = "파티에 참가 신청을 합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "신청 성공",
                    content = @Content(schema = @Schema(implementation = ApplyToPartyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이미 신청함",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "ALREADY_APPLIED",
                                                    "status": "400",
                                                    "message": "이미 신청한 파티입니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "본인 파티 신청 불가",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "CANNOT_APPLY_OWN_PARTY",
                                                    "status": "400",
                                                    "message": "본인이 만든 파티에는 신청할 수 없습니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "파티 인원 가득 참",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "PARTY_FULL",
                                                    "status": "400",
                                                    "message": "파티 인원이 가득 찼습니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "파티를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "PARTY_NOT_FOUND",
                                            "status": "404",
                                            "message": "파티를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<ApplyToPartyResponse> applyParty(@PathVariable("partyId") Long partyId) {
        return ResponseEntity.ok().body(
                new ApplyToPartyResponse("홍길동", 23, Gender.FEMALE, "BTS 공연")
        );
    }

    @DeleteMapping("/{partyId}/application/{applicationId}/cancel")
    @Operation(summary = "파티 참가 신청 취소", description = "파티 참가 신청을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "CANNOT_CANCEL_APPROVED_APPLICATION",
                                            "status": "400",
                                            "message": "승인된 신청은 취소할 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "신청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "APPLICATION_NOT_FOUND",
                                            "status": "404",
                                            "message": "신청 내역을 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<Void> cancelAppliedParty(
            @PathVariable("partyId") Long partyId,
            @PathVariable("applicationId") Long applicationId
    ) {
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{partyId}/application/{applicationId}/accept")
    @Operation(summary = "파티 신청 승인", description = "파티 신청을 승인합니다. 파티장만 승인할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "승인 성공",
                    content = @Content(schema = @Schema(implementation = AcceptApplicationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "파티 인원 가득 참",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "PARTY_FULL",
                                                    "status": "400",
                                                    "message": "파티 인원이 가득 찼습니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "이미 승인됨",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "APPLICATION_ALREADY_APPROVED",
                                                    "status": "400",
                                                    "message": "이미 승인된 신청입니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED_PARTY_LEADER",
                                            "status": "403",
                                            "message": "파티장만 이 작업을 수행할 수 있습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "파티를 찾을 수 없음",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "PARTY_NOT_FOUND",
                                                    "status": "404",
                                                    "message": "파티를 찾을 수 없습니다."
                                                  }
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "신청을 찾을 수 없음",
                                            value = """
                                                {
                                                  "error": {
                                                    "code": "APPLICATION_NOT_FOUND",
                                                    "status": "404",
                                                    "message": "신청 내역을 찾을 수 없습니다."
                                                  }
                                                }
                                                """
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<AcceptApplicationResponse> acceptPartyApplicant(
            @PathVariable("partyId") Long partyId,
            @PathVariable("applicationId") Long applicationId
    ) {
        return ResponseEntity.ok().body(new AcceptApplicationResponse(1L, "BTS 공연", 1L));
    }

    @PatchMapping("/{partyId}/application/{applicationId}/reject")
    @Operation(summary = "파티 신청 거절", description = "파티 신청을 거절합니다. 파티장만 거절할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "거절 성공",
                    content = @Content(schema = @Schema(implementation = RejectApplicationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "APPLICATION_ALREADY_REJECTED",
                                            "status": "400",
                                            "message": "이미 거절된 신청입니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED_PARTY_LEADER",
                                            "status": "403",
                                            "message": "파티장만 이 작업을 수행할 수 있습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "리소스를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "APPLICATION_NOT_FOUND",
                                            "status": "404",
                                            "message": "신청 내역을 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<RejectApplicationResponse> rejectPartyApplicant(
            @PathVariable("partyId") Long partyId,
            @PathVariable("applicationId") Long applicationId
    ) {
        return ResponseEntity.ok().body(new RejectApplicationResponse(1L, "BTS 공연", 1L));
    }

    @GetMapping("/{partyId}/application/applicants")
    @Operation(summary = "파티 신청자 목록 조회", description = "파티에 신청한 사람들의 목록을 조회합니다. 파티장만 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetApplicantsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "UNAUTHORIZED_PARTY_LEADER",
                                            "status": "403",
                                            "message": "파티장만 이 작업을 수행할 수 있습니다."
                                          }
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "파티를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "PARTY_NOT_FOUND",
                                            "status": "404",
                                            "message": "파티를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<GetApplicantsResponse> getPartyApplicants(
            @PathVariable("partyId") Long partyId
    ) {
        GetApplicantsResponse response = new GetApplicantsResponse(
                1L,
                List.of(
                        new GetApplicantsResponse.ApplicationInfo(
                                101L,
                                new GetApplicantsResponse.ApplicantInfo(
                                        200L,
                                        "명란 최애",
                                        "https://example.com/profile.jpg",
                                        Gender.FEMALE,
                                        23
                                ),
                                ApplicationStatus.PENDING
                        ),
                        new GetApplicantsResponse.ApplicationInfo(
                                102L,
                                new GetApplicantsResponse.ApplicantInfo(
                                        201L,
                                        "팝맨틴 풀",
                                        "https://example.com/profile2.jpg",
                                        Gender.FEMALE,
                                        25
                                ),
                                ApplicationStatus.PENDING
                        ),
                        new GetApplicantsResponse.ApplicationInfo(
                                103L,
                                new GetApplicantsResponse.ApplicantInfo(
                                        202L,
                                        "명젯",
                                        "https://example.com/profile3.jpg",
                                        Gender.FEMALE,
                                        22
                                ),
                                ApplicationStatus.PENDING
                        )
                ),
                new GetApplicantsResponse.ApplicationSummary(
                        5,
                        3,
                        2,
                        0
                )
        );

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{partyId}/members")
    @Operation(summary = "파티 확정 멤버 목록 조회", description = "파티에 확정된 멤버 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartyMembersResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "파티를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": {
                                            "code": "PARTY_NOT_FOUND",
                                            "status": "404",
                                            "message": "파티를 찾을 수 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<GetPartyMembersResponse> getPartyMembers(
            @PathVariable("partyId") Long partyId
    ) {
        GetPartyMembersResponse response = new GetPartyMembersResponse(
                1L,
                List.of(
                        new GetPartyMembersResponse.MemberInfo(
                                1L,
                                100L,
                                "나",
                                "https://example.com/profile.jpg",
                                "파티장"
                        ),
                        new GetPartyMembersResponse.MemberInfo(
                                2L,
                                200L,
                                "팝단 최애",
                                "https://example.com/profile2.jpg",
                                "멤버"
                        ),
                        new GetPartyMembersResponse.MemberInfo(
                                3L,
                                201L,
                                "팝복단 팬",
                                "https://example.com/profile3.jpg",
                                "멤버"
                        )
                ),
                3
        );

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/user/me/party/application")
    @Operation(summary = "신청한 파티 목록 조회", description = "내가 신청한 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyApplicationsResponse.class))
            )
    })
    public ResponseEntity<GetMyApplicationsResponse> getAppliedParty(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        GetMyApplicationsResponse response = new GetMyApplicationsResponse(
                List.of(
                        // 1. 대기중인 신청
                        new GetMyApplicationsResponse.ApplicationItem(
                                101L,
                                new GetMyApplicationsResponse.PartyInfo(
                                        10L,
                                        new GetMyApplicationsResponse.LeaderInfo(
                                                100L,
                                                "지민이 최애",
                                                "https://example.com/profile.jpg"
                                        ),
                                        new GetMyApplicationsResponse.EventInfo(
                                                123L,
                                                "BTS WORLD TOUR 2025",
                                                "잠실종합운동장",
                                                LocalDateTime.of(2025, 12, 15, 19, 0, 0)
                                        ),
                                        new GetMyApplicationsResponse.PartyDetailInfo(
                                                "출발팟",
                                                "강남역 3번출구",
                                                2,
                                                4
                                        )
                                ),
                                "대기중",
                                null
                        ),

                        // 2. 승인된 신청
                        new GetMyApplicationsResponse.ApplicationItem(
                                102L,
                                new GetMyApplicationsResponse.PartyInfo(
                                        11L,
                                        new GetMyApplicationsResponse.LeaderInfo(
                                                101L,
                                                "뉴진스와 함께",
                                                "https://example.com/profile2.jpg"
                                        ),
                                        new GetMyApplicationsResponse.EventInfo(
                                                124L,
                                                "NewJeans 미니 3집 발매 팬사인회",
                                                "예스24 라이브홀",
                                                LocalDateTime.of(2025, 12, 5, 14, 0, 0)
                                        ),
                                        new GetMyApplicationsResponse.PartyDetailInfo(
                                                "복귀팟",
                                                "예스24 라이브홀",
                                                3,
                                                4
                                        )
                                ),
                                "승인",
                                60L
                        )
                ),
                2,
                1,
                0
        );

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/user/me/party/created")
    @Operation(summary = "만든 파티 목록 조회", description = "내가 만든 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyCreatedPartiesResponse.class))
            )
    })
    public ResponseEntity<GetMyCreatedPartiesResponse> getCreatedParty(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size
    ) {
        GetMyCreatedPartiesResponse response = new GetMyCreatedPartiesResponse(
                List.of(
                        // 1. 첫 번째 파티 - 지민이 최애
                        new GetMyCreatedPartiesResponse.CreatedPartyItem(
                                1L,
                                new GetMyCreatedPartiesResponse.EventInfo(
                                        123L,
                                        "BTS WORLD TOUR 2025",
                                        "2025-12-15",
                                        "잠실종합운동장",
                                        "방탄소년단"
                                ),
                                new GetMyCreatedPartiesResponse.PartyDetailInfo(
                                        "출발팟",
                                        "강남역 3번출구",
                                        "잠실종합운동장",
                                        "택시",
                                        4,
                                        2,
                                        "모집중"
                                ),
                                new GetMyCreatedPartiesResponse.ApplicationStatistics(
                                        3,
                                        2,
                                        1
                                ),
                                "20대 여성분들 함께해요! 편하게 가실 거예요~",
                                50L,
                                LocalDateTime.of(2025, 12, 1, 10, 0, 0)
                        ),

                        // 2. 두 번째 파티 - 뉴진스와 함께
                        new GetMyCreatedPartiesResponse.CreatedPartyItem(
                                2L,
                                new GetMyCreatedPartiesResponse.EventInfo(
                                        124L,
                                        "NewJeans 미니 3집 발매 팬사인회",
                                        "2025-12-05",
                                        "예스24 라이브홀",
                                        "뉴진스"
                                ),
                                new GetMyCreatedPartiesResponse.PartyDetailInfo(
                                        "복귀팟",
                                        "예스24 라이브홀",
                                        "을대입구역",
                                        "택시",
                                        4,
                                        3,
                                        "모집중"
                                ),
                                new GetMyCreatedPartiesResponse.ApplicationStatistics(
                                        7,
                                        3,
                                        2
                                ),
                                "NewJeans 팬사인회 끝나고 같이 갈 분 구해요~ 뉴진스 팬분만 받겠습니다~",
                                51L,
                                LocalDateTime.of(2025, 11, 28, 16, 0, 0)
                        )
                ),
                2,
                1,
                0
        );

        return ResponseEntity.ok().body(response);
    }
}