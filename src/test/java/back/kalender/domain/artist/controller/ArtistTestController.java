package back.kalender.domain.artist.controller;

import back.kalender.domain.artist.entity.Artist;
import back.kalender.domain.artist.entity.ArtistFollow;
import back.kalender.domain.artist.repository.ArtistFollowRepository;
import back.kalender.domain.artist.repository.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ArtistTestController {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistFollowRepository artistFollowRepository;

    @BeforeEach
    void before() {
        // 더미 데이터 삽입
        artistRepository.save(new Artist("NewJeans", "https://img.com/nj"));
        artistRepository.save(new Artist("IVE", "https://img.com/ive"));
    }

    @Test
    @DisplayName("t1 — 전체 아티스트 조회")
    void t1() throws Exception {

        ResultActions resultActions = mvc
                .perform(get("/api/v1/artist"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists.length()").value(2)) // Dummy 포함
                .andExpect(jsonPath("$.artists[0].name").exists());
    }

    @Test
    @DisplayName("t2 — 팔로우한 아티스트 조회")
    void t2() throws Exception {

        Long userId = 1L;
        Artist artist = artistRepository.findAll().get(0);
        artistFollowRepository.save(new ArtistFollow(userId, artist));

        ResultActions resultActions = mvc
                .perform(get("/api/v1/artist/following"))
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].name").value(artist.getName()));
    }

    @Test
    @DisplayName("t3 — 아티스트 팔로우 성공")
    void t3() throws Exception {

        Long artistId = artistRepository.findAll().get(0).getId();

        ResultActions resultActions = mvc
                .perform(post("/api/v1/artist/{id}/follow", artistId))
                .andDo(print());

        resultActions
                .andExpect(status().isOk());

        assertThat(artistFollowRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("t4 — 이미 팔로우한 아티스트 팔로우 시 예외")
    void t4() throws Exception {

        Long userId = 1L;
        Artist artist = artistRepository.findAll().get(0);
        artistFollowRepository.save(new ArtistFollow(userId, artist));

        ResultActions resultActions = mvc
                .perform(post("/api/v1/artist/{id}/follow", artist.getId()))
                .andDo(print());

        resultActions
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("t5 — 언팔로우 성공")
    void t5() throws Exception {

        Long userId = 1L;
        Artist artist = artistRepository.findAll().get(0);
        artistFollowRepository.save(new ArtistFollow(userId, artist));

        ResultActions resultActions = mvc
                .perform(delete("/api/v1/artist/{id}/unfollow", artist.getId()))
                .andDo(print());

        resultActions
                .andExpect(status().isNoContent());

        assertThat(artistFollowRepository.count()).isEqualTo(0);
    }

    // ------------------------------------------------------------
    // t6 — 팔로우 상태가 아닌 경우 언팔로우 시 예외
    // ------------------------------------------------------------
    @Test
    @DisplayName("t6 — 팔로우 상태가 아닌 경우 언팔로우 예외")
    void t6() throws Exception {

        Long artistId = artistRepository.findAll().get(0).getId();

        ResultActions resultActions = mvc
                .perform(delete("/api/v1/artist/{id}/unfollow", artistId))
                .andDo(print());

        resultActions
                .andExpect(status().isBadRequest());
    }
}
