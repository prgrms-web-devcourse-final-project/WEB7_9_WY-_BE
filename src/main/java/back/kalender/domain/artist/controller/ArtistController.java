package back.kalender.domain.artist.controller;

import back.kalender.domain.artist.dto.response.ArtistListResponse;
import back.kalender.domain.artist.dto.response.ArtistResponse;
import back.kalender.domain.artist.service.ArtistService;
import back.kalender.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/artist")
@RequiredArgsConstructor
public class ArtistController implements ArtistControllerSpec {

    private final ArtistService artistService;

    @GetMapping
    @Override
    public ResponseEntity<ArtistListResponse> getAllArtists() {
        List<ArtistResponse> artistResponses = artistService.getAllArtists();
        return ResponseEntity.ok(new ArtistListResponse(artistResponses));
    }

    @GetMapping("/following")
    @Override
    public ResponseEntity<ArtistListResponse> getFollowingArtists(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ArtistResponse> responses =
                artistService.getAllFollowedArtists(userDetails.getUserId());
        return ResponseEntity.ok(new ArtistListResponse(responses));
    }

    @PostMapping("/{artistId}/follow")
    @Override
    public ResponseEntity<Void> followArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        artistService.followArtist(userDetails.getUserId(), artistId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{artistId}/unfollow")
    @Override
    public ResponseEntity<Void> unfollowArtist(
            @PathVariable Long artistId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        artistService.unfollowArtist(userDetails.getUserId(), artistId);
        return ResponseEntity.noContent().build();
    }
}
