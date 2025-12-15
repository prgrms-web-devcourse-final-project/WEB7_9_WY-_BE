package back.kalender.domain.artist.controller;

import back.kalender.domain.artist.dto.response.ArtistListResponse;
import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.service.ArtistService;
import back.kalender.global.exception.ErrorResponse;
import back.kalender.global.security.user.CustomUserDetails;
import back.kalender.global.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Artist", description = "아티스트 조회 및 팔로우 관련 API")
@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    // ✅ 전체 아티스트 조회 (인증 불필요)
    @Operation(summary = "전체 아티스트 조회", description = "등록된 전체 아티스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ArtistListResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ArtistListResponse> getAllArtists() {
        log.debug("Request: getAllArtists");

        List<ArtistResponse> artistResponses = artistService.getAllArtists();

        log.info("Response: getAllArtists success, size={}", artistResponses.size());
        return ResponseEntity.ok(new ArtistListResponse(artistResponses));
    }

    // ✅ 팔로우한 아티스트 조회 (인증 필요)
    @Operation(summary = "팔로우한 아티스트 조회", description = "사용자가 팔로우한 아티스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ArtistListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/following")
    public ResponseEntity<ArtistListResponse> getFollowingArtists(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        log.info("Request: getFollowingArtists userId={}", userDetails.getUserId());

        List<ArtistResponse> followedArtistResponses =
                artistService.getAllFollowedArtists(userDetails.getUserId());

        log.info(
                "Response: getFollowingArtists success, userId={}, size={}",
                userDetails.getUserId(),
                followedArtistResponses.size()
        );

        return ResponseEntity.ok(new ArtistListResponse(followedArtistResponses));
    }

    // ✅ 아티스트 팔로우
    @Operation(summary = "아티스트 팔로우", description = "사용자가 특정 아티스트를 팔로우합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "아티스트를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ARTIST_NOT_FOUND",
                                    value = """
                                    {
                                      "error": {
                                        "code": "ARTIST_NOT_FOUND",
                                        "status": "404",
                                        "message": "아티스트를 찾을 수 없습니다."
                                      }
                                    }
                                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 팔로우한 아티스트",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ALREADY_FOLLOWED",
                                    value = """
                                    {
                                      "error": {
                                        "code": "ALREADY_FOLLOWED",
                                        "status": "409",
                                        "message": "이미 팔로우한 아티스트입니다."
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    @PostMapping("/{artistId}/follow")
    public ResponseEntity<Void> followArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        log.info("Request: followArtist userId={}, artistId={}", userDetails.getUserId(), artistId);

        artistService.followArtist(userDetails.getUserId(), artistId);

        log.info("Response: followArtist success userId={}, artistId={}", userDetails.getUserId(), artistId);
        return ResponseEntity.ok().build();
    }

    // ✅ 아티스트 언팔로우
    @Operation(summary = "아티스트 언팔로우", description = "사용자가 특정 아티스트 팔로우를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "언팔로우 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "팔로우 상태가 아님",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "ARTIST_NOT_FOLLOWED",
                                    value = """
                                    {
                                      "error": {
                                        "code": "ARTIST_NOT_FOLLOWED",
                                        "status": "400",
                                        "message": "팔로우 상태가 아닙니다."
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    @DeleteMapping("/{artistId}/unfollow")
    public ResponseEntity<Void> unfollowArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        log.info("Request: unfollowArtist userId={}, artistId={}", userDetails.getUserId(), artistId);

        artistService.unfollowArtist(userDetails.getUserId(), artistId);

        log.info("Response: unfollowArtist success userId={}, artistId={}", userDetails.getUserId(), artistId);
        return ResponseEntity.noContent().build();
    }
}
