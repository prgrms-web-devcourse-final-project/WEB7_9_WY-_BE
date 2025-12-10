package back.kalender.domain.party.controller;

import back.kalender.domain.party.dto.request.CreatePartyRequest;
import back.kalender.domain.party.dto.request.UpdatePartyRequest;
import back.kalender.domain.party.dto.response.*;
import back.kalender.domain.party.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Party", description = "파티 관련 API")
@RestController
@RequestMapping("/api/v1/party")
@RequiredArgsConstructor
@Validated
public class PartyController {

    private final PartyService partyService;

    @Operation(summary = "파티 생성", description = "새로운 파티를 생성합니다.")
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
    public ResponseEntity<CreatePartyResponse> createParty(
            @Valid @RequestBody CreatePartyRequest request
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        CreatePartyResponse response = partyService.createParty(request, currentUserId);
        return ResponseEntity.ok(response);
    }

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
    @PutMapping("/{partyId}")
    public ResponseEntity<UpdatePartyResponse> updateParty(
            @PathVariable Long partyId,
            @Valid @RequestBody UpdatePartyRequest request
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        UpdatePartyResponse response = partyService.updateParty(partyId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "파티 삭제", description = "파티를 삭제합니다. 파티장만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "3105",
                              "message": "파티장만 파티를 삭제할 수 있습니다."
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
    public ResponseEntity<Void> deleteParty(
            @PathVariable Long partyId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        partyService.deleteParty(partyId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "파티 목록 조회", description = "파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPartiesResponse.class)))
    })
    @GetMapping
    public ResponseEntity<GetPartiesResponse> getParties(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        Pageable pageable = PageRequest.of(page, size);
        GetPartiesResponse response = partyService.getParties(pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<ApplyToPartyResponse> applyToParty(
            @PathVariable Long partyId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        ApplyToPartyResponse response = partyService.applyToParty(partyId, currentUserId);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<Void> cancelApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        partyService.cancelApplication(partyId, applicationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "파티 신청 승인", description = "파티 신청을 승인합니다. 파티장만 승인할 수 있습니다.")
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
    public ResponseEntity<AcceptApplicationResponse> acceptApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        AcceptApplicationResponse response = partyService.acceptApplication(partyId, applicationId, currentUserId);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<RejectApplicationResponse> rejectApplication(
            @PathVariable Long partyId,
            @PathVariable Long applicationId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        RejectApplicationResponse response = partyService.rejectApplication(partyId, applicationId, currentUserId);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<GetApplicantsResponse> getApplicants(
            @PathVariable Long partyId
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        GetApplicantsResponse response = partyService.getApplicants(partyId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "파티 확정 멤버 목록 조회", description = "파티에 확정된 멤버 목록을 조회합니다.")
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
    public ResponseEntity<GetPartyMembersResponse> getPartyMembers(
            @PathVariable Long partyId
    ) {
        GetPartyMembersResponse response = partyService.getPartyMembers(partyId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "신청한 파티 목록 조회", description = "내가 신청한 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyApplicationsResponse.class)))
    })
    @GetMapping("/user/me/party/application")
    public ResponseEntity<GetMyApplicationsResponse> getMyApplications(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        Pageable pageable = PageRequest.of(page, size);
        GetMyApplicationsResponse response = partyService.getMyApplications(status, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "만든 파티 목록 조회", description = "내가 만든 파티 목록을 조회합니다. 페이징 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetMyCreatedPartiesResponse.class)))
    })
    @GetMapping("/user/me/party/created")
    public ResponseEntity<GetMyCreatedPartiesResponse> getMyCreatedParties(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long currentUserId = 1L; // TODO: SecurityContext에서 가져오기
        Pageable pageable = PageRequest.of(page, size);
        GetMyCreatedPartiesResponse response = partyService.getMyCreatedParties(status, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }
}