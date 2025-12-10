package back.kalender.domain.schedule.service;

import back.kalender.domain.artist.entity.ArtistFollowTmp;
import back.kalender.domain.artist.entity.ArtistTmp;
import back.kalender.domain.artist.repository.ArtistFollowRepositoryTmp;
import back.kalender.domain.schedule.dto.response.MonthlyScheduleItem;
import back.kalender.domain.schedule.dto.response.MonthlySchedulesResponse;
import back.kalender.domain.schedule.entity.ScheduleCategory;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 테스트")
public class ScheduleServiceImplTest {
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ArtistFollowRepositoryTmp artistFollowRepository;
    @InjectMocks
    private ScheduleServiceImpl scheduleServiceImpl;

    @Test
    @DisplayName("getFollowingSchedules(팔로우한 전체 아티스트 일정 조회) 테스트")
    void getFollowingSchedules_Success() {
        Long userId = 1L;

        ArtistTmp bts = new ArtistTmp("BTS", "image_url");
        ArtistTmp bp = new ArtistTmp("Black Pink", "image_url");
        ArtistTmp njz = new ArtistTmp("New Jeans", "image_url");
        ArtistTmp nct = new ArtistTmp("NCT WISH", "image_url");

        ReflectionTestUtils.setField(bts, "id", 1L);
        ReflectionTestUtils.setField(bp, "id", 2L);
        ReflectionTestUtils.setField(njz, "id", 3L);
        ReflectionTestUtils.setField(nct, "id", 4L);

        ArtistFollowTmp f1 = new ArtistFollowTmp(userId, bts);
        ArtistFollowTmp f2 = new ArtistFollowTmp(userId, bp);
        ArtistFollowTmp f3 = new ArtistFollowTmp(userId, njz);

        given(artistFollowRepository.findAllByUserId(userId))
                .willReturn(List.of(f1, f2, f3));

        List<MonthlyScheduleItem> dbResult = List.of(
                createDto(100L, 1L, "BTS", "콘서트"),       // BTS 일정
                createDto(101L, 2L, "Black Pink", "컴백"), // BP 일정
                createDto(102L, 3L, "New Jeans", "팬미팅") // NJZ 일정
        );

        given(scheduleRepository.findMonthlySchedules(any(), any(), any()))
                .willReturn(dbResult);

        MonthlySchedulesResponse response = scheduleServiceImpl.getFollowingSchedules(userId, 2025, 12);

        assertThat(response.schedules()).hasSize(3);

        MonthlyScheduleItem item1 = response.schedules().get(0);
        assertThat(item1.artistName()).isEqualTo("BTS");
        assertThat(item1.title()).isEqualTo("콘서트");

        MonthlyScheduleItem item2 = response.schedules().get(1);
        assertThat(item2.artistName()).isEqualTo("Black Pink");
        assertThat(item2.title()).isEqualTo("컴백");

        MonthlyScheduleItem item3 = response.schedules().get(2);
        assertThat(item3.artistName()).isEqualTo("New Jeans");
        assertThat(item3.title()).isEqualTo("팬미팅");
    }

    private MonthlyScheduleItem createDto(Long id, Long artistId, String artistName, String title) {
        return new MonthlyScheduleItem(
                id, artistId, artistName, title,
                ScheduleCategory.CONCERT,
                LocalDateTime.now(),
                Optional.empty(),
                LocalDate.now(),
                Optional.empty()
        );
    }
}
