package back.kalender.domain.artist.controller;

import back.kalender.domain.artist.dto.response.ArtistListResponse;
import back.kalender.global.exception.ErrorResponse;
import back.kalender.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

public interface ArtistControllerSpec {

    @Operation(summary = "전체 아티스트 조회", description = "등록된 전체 아티스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ArtistListResponse.class))
            )
    })
    ResponseEntity<ArtistListResponse> getAllArtists();


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
    ResponseEntity<ArtistListResponse> getFollowingArtists(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );


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
    ResponseEntity<Void> followArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );


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
    ResponseEntity<Void> unfollowArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
