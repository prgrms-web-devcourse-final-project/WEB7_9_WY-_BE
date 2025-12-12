package back.kalender.domain.artist.controller;

import back.kalender.domain.artist.dto.response.ArtistListResponse;
import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.service.ArtistService;
import back.kalender.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//  TODO: 로그 작성 및 apiresponse 에러응답 수정 및 test코드 수정

@Tag(name = "Artist", description = "아티스트 조회 및 팔로우 관련 API")
@RestController
@RequestMapping("api/v1/artist")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @Operation(summary = "전체 아티스트 조회", description = "등록된 전체 아티스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ArtistListResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ArtistListResponse> getAllArtists() {
        List<ArtistResponse> artistResponses = artistService.getAllArtists();
        return ResponseEntity.ok(new ArtistListResponse(artistResponses));
    }

    @Operation(summary = "팔로우한 아티스트 조회", description = "사용자가 팔로우한 아티스트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ArtistListResponse.class)))
    })
    @GetMapping("/following")
    public ResponseEntity<ArtistListResponse> getFollowingArtists() {
        Long userId = 1L;
        List<ArtistResponse> followedArtistResponses = artistService.getAllFollowedArtists(userId);
        return ResponseEntity.ok(new ArtistListResponse(followedArtistResponses));
    }

    @Operation(summary = "아티스트 팔로우", description = "사용자가 특정 아티스트를 팔로우합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "팔로우 성공"),
            @ApiResponse(responseCode = "404", description = "아티스트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 팔로우 중",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{artistId}/follow")
    public ResponseEntity<Void> followArtist(
            @PathVariable Long artistId) {
        Long userId = 1L;
        artistService.followArtist(userId,artistId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "아티스트 언팔로우", description = "사용자가 특정 아티스트 팔로우를 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "언팔로우 성공"),
            @ApiResponse(responseCode = "400", description = "팔로우 상태가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{artistId}/unfollow")
    public ResponseEntity<Void> unfollowArtist(
            @PathVariable Long artistId) {
        Long userId = 1L;
        artistService.unfollowArtist(userId,artistId);
        return ResponseEntity.noContent().build();
    }

}
