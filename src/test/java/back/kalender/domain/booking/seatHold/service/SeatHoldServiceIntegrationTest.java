//package back.kalender.domain.booking.seatHold.service;
//
//import back.kalender.domain.booking.performanceSeat.entity.PerformanceSeat;
//import back.kalender.domain.booking.performanceSeat.entity.SeatStatus;
//import back.kalender.domain.booking.performanceSeat.repository.PerformanceSeatRepository;
//import back.kalender.domain.booking.reservation.dto.request.HoldSeatsRequest;
//import back.kalender.domain.booking.reservation.dto.request.ReleaseSeatsRequest;
//import back.kalender.domain.booking.reservation.entity.Reservation;
//import back.kalender.domain.booking.reservation.entity.ReservationStatus;
//import back.kalender.domain.booking.reservation.repository.ReservationRepository;
//import back.kalender.domain.booking.reservationSeat.entity.ReservationSeat;
//import back.kalender.domain.booking.reservationSeat.repository.ReservationSeatRepository;
//import back.kalender.domain.booking.seatHold.exception.SeatHoldConflictException;
//import back.kalender.domain.performance.priceGrade.entity.PriceGrade;
//import back.kalender.domain.performance.priceGrade.repository.PriceGradeRepository;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.*;
//import static org.assertj.core.api.Assertions.*;
//
//@Testcontainers
//@SpringBootTest
//@ActiveProfiles("test")
//@DisplayName("SeatHoldService 통합 테스트")
//class SeatHoldServiceIntegrationTest {
//
//    @Container
//    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
//            .withExposedPorts(6379);
//
//    @DynamicPropertySource
//    static void redisProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.redis.host", redisContainer::getHost);
//        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
//    }
//
//    @Autowired
//    private SeatHoldService seatHoldService;
//
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;
//
//    @Autowired
//    private ReservationRepository reservationRepository;
//
//    @Autowired
//    private PerformanceSeatRepository performanceSeatRepository;
//
//    @Autowired
//    private ReservationSeatRepository reservationSeatRepository;
//
//    @Autowired
//    private PriceGradeRepository priceGradeRepository;
//
//    private static final Long SCHEDULE_ID = 10L;
//    private static final Long USER_A = 100L;
//    private static final Long USER_B = 200L;
//
//    private PriceGrade grade;
//    private PerformanceSeat seat1;
//    private PerformanceSeat seat2;
//
//    private String ownerKey(Long scheduleId, Long seatId) {
//        return String.format("seat:hold:owner:%d:%d", scheduleId, seatId);
//    }
//
//    @BeforeEach
//    void setUp() {
//        reservationSeatRepository.deleteAllInBatch();
//        reservationRepository.deleteAllInBatch();
//        performanceSeatRepository.deleteAllInBatch();
//        priceGradeRepository.deleteAllInBatch();
//
//        deleteRedisKeysLike("seat:hold:owner:" + SCHEDULE_ID + ":*");
//        deleteRedisKeysLike("seat:lock:" + SCHEDULE_ID + ":*");
//        deleteRedisKeysLike("seat:sold:" + SCHEDULE_ID);
//        deleteRedisKeysLike("seat:version:" + SCHEDULE_ID);
//        deleteRedisKeysLike("seat:changes:" + SCHEDULE_ID + ":*");
//
//
//        // PriceGrade 생성
//        grade = priceGradeRepository.save(new PriceGrade(1L, "VIP", 200_000));
//
//        // PerformanceSeat 생성
//        seat1 = performanceSeatRepository.save(
//                PerformanceSeat.create(SCHEDULE_ID, 1L, grade.getId(), 1, "A", 1, 1, 10, 10)
//        );
//        seat2 = performanceSeatRepository.save(
//                PerformanceSeat.create(SCHEDULE_ID, 2L, grade.getId(), 1, "A", 1, 2, 20, 10)
//        );
//    }
//
//
//
//    @Test
//    @DisplayName("HOLD 성공 - 2좌석 HOLD + ReservationSeat 생성 + 총액 반영")
//    void holdSeats_success() {
//        Reservation reservation = reservationRepository.save(Reservation.create(USER_A, SCHEDULE_ID));
//
//        HoldSeatsRequest req = new HoldSeatsRequest(List.of(seat1.getId(), seat2.getId()));
//        var res = seatHoldService.holdSeats(reservation.getId(), req, USER_A);
//
//        // DB 좌석 상태 HOLD 확인
//        PerformanceSeat s1 = performanceSeatRepository.findById(seat1.getId()).orElseThrow();
//        PerformanceSeat s2 = performanceSeatRepository.findById(seat2.getId()).orElseThrow();
//        assertThat(s1.getStatus()).isEqualTo(SeatStatus.HOLD);
//        assertThat(s2.getStatus()).isEqualTo(SeatStatus.HOLD);
//        assertThat(s1.getHoldUserId()).isEqualTo(USER_A);
//        assertThat(s2.getHoldUserId()).isEqualTo(USER_A);
//
//        // ReservationSeat 생성 확인
//        List<ReservationSeat> seats = reservationSeatRepository.findByReservationId(reservation.getId());
//        assertThat(seats).hasSize(2);
//
//        // Reservation 총액 반영 확인
//        Reservation updated = reservationRepository.findById(reservation.getId()).orElseThrow();
//        assertThat(updated.getTotalAmount()).isEqualTo(400_000);
//        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.HOLD);
//
//        // Redis owner 키 생성 확인
//        assertThat(getRedisOwner(seat1.getId())).isEqualTo(USER_A.toString());
//        assertThat(getRedisOwner(seat2.getId())).isEqualTo(USER_A.toString());
//
//        // 응답도 기본적으로 좌석수/상태 등을 담을테니 최소 sanity check
//        assertThat(res).isNotNull();
//    }
//
//    @Test
//    @DisplayName("HOLD 충돌 - 이미 다른 사용자가 HOLD한 좌석 포함 시 전체 실패 + 롤백(ReservationSeat/DB상태) 확인")
//    void holdSeats_conflict_rollsBack() {
//        // A가 seat1 HOLD
//        Reservation ra = reservationRepository.save(Reservation.create(USER_A, SCHEDULE_ID));
//        seatHoldService.holdSeats(ra.getId(), new HoldSeatsRequest(List.of(seat1.getId())), USER_A);
//
//        // B가 seat1 + seat2를 동시에 시도 -> seat1에서 충돌 -> 전체 실패 + 롤백되어야 함
//        Reservation rb = reservationRepository.save(Reservation.create(USER_B, SCHEDULE_ID));
//
//        assertThatThrownBy(() ->
//                seatHoldService.holdSeats(rb.getId(), new HoldSeatsRequest(List.of(seat1.getId(), seat2.getId())), USER_B)
//        ).isInstanceOf(SeatHoldConflictException.class);
//
//        // B의 ReservationSeat는 남으면 안 됨
//        assertThat(reservationSeatRepository.findByReservationId(rb.getId())).isEmpty();
//
//        // seat2는 B가 잡으려다 롤백되어 AVAILABLE 이어야 함
//        PerformanceSeat s2 = performanceSeatRepository.findById(seat2.getId()).orElseThrow();
//        assertThat(s2.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
//
//        // seat1은 A가 HOLD 유지
//        PerformanceSeat s1 = performanceSeatRepository.findById(seat1.getId()).orElseThrow();
//        assertThat(s1.getStatus()).isEqualTo(SeatStatus.HOLD);
//        assertThat(getRedisOwner(seat1.getId())).isEqualTo(USER_A.toString());
//    }
//
//    @Test
//    @DisplayName("RELEASE 성공 - 일부 Redis owner 키가 사라진 상태(만료/유실)여도 DB를 정리하며 전체 해제")
//    void releaseSeats_success_evenIfRedisOwnerMissing() {
//        Reservation r = reservationRepository.save(Reservation.create(USER_A, SCHEDULE_ID));
//        seatHoldService.holdSeats(r.getId(), new HoldSeatsRequest(List.of(seat1.getId(), seat2.getId())), USER_A);
//
//        // seat2의 Redis owner 키를 강제로 삭제(= TTL 만료/유실 상황)
//        deleteOwnerKey(seat2.getId());
//
//        ReleaseSeatsRequest req = new ReleaseSeatsRequest(List.of(seat1.getId(), seat2.getId()));
//
//        var res = seatHoldService.releaseSeats(r.getId(), req, USER_A);
//
//        PerformanceSeat s1 = performanceSeatRepository.findById(seat1.getId()).orElseThrow();
//        PerformanceSeat s2 = performanceSeatRepository.findById(seat2.getId()).orElseThrow();
//
//        assertThat(s1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
//        assertThat(s2.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
//
//        assertThat(getRedisOwner(seat1.getId())).isNull();
//        assertThat(getRedisOwner(seat2.getId())).isNull();
//
//        // reservation seats 삭제 확인
//        assertThat(reservationSeatRepository.findByReservationId(r.getId())).isEmpty();
//
//        // reservation 상태 cancel 확인
//        Reservation updated = reservationRepository.findById(r.getId()).orElseThrow();
//        assertThat(updated.getStatus().name()).contains("CANCELLED");
//
//        assertThat(res).isNotNull();
//    }
//
//    @Test
//    @DisplayName("동시성 - 여러 예약(여러 사용자)이 동일 좌석을 동시에 HOLD 시도하면 딱 1명만 성공")
//    void concurrent_hold_singleWinner() throws Exception {
//        int threadCount = 20;
//
//        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch ready = new CountDownLatch(threadCount);
//        CountDownLatch start = new CountDownLatch(1);
//        CountDownLatch done = new CountDownLatch(threadCount);
//
//        List<Long> userIds = new ArrayList<>();
//        List<Long> reservationIds = new ArrayList<>();
//
//        for (int i = 0; i < threadCount; i++) {
//            long uid = 1000L + i;
//            userIds.add(uid);
//            Reservation r = reservationRepository.save(Reservation.create(uid, SCHEDULE_ID));
//            reservationIds.add(r.getId());
//        }
//
//        ConcurrentLinkedQueue<Long> winners = new ConcurrentLinkedQueue<>();
//        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
//
//        for (int i = 0; i < threadCount; i++) {
//            final Long uid = userIds.get(i);
//            final Long rid = reservationIds.get(i);
//
//            pool.submit(() -> {
//                ready.countDown();
//                try {
//                    start.await();
//
//                    seatHoldService.holdSeats(
//                            rid,
//                            new HoldSeatsRequest(List.of(seat1.getId())),
//                            uid
//                    );
//                    winners.add(uid);
//                } catch (Throwable t) {
//                    errors.add(t);
//                } finally {
//                    done.countDown();
//                }
//                return null;
//            });
//        }
//
//        ready.await(5, TimeUnit.SECONDS);
//        start.countDown();
//        done.await(10, TimeUnit.SECONDS);
//        pool.shutdownNow();
//
//        // 정확히 1명만 성공해야 함
//        assertThat(winners).hasSize(1);
//
//        Long winnerId = winners.peek();
//        assertThat(winnerId).isNotNull();
//
//        // DB / Redis owner도 winner로 유지되어야 함
//        PerformanceSeat s1 = performanceSeatRepository.findById(seat1.getId()).orElseThrow();
//        assertThat(s1.getStatus()).isEqualTo(SeatStatus.HOLD);
//        assertThat(s1.getHoldUserId()).isEqualTo(winnerId);
//
//        assertThat(getRedisOwner(seat1.getId())).isEqualTo(winnerId.toString());
//    }
//
//    private String getRedisOwner(Long seatId) {
//        String key = String.format("seat:hold:owner:%d:%d", SCHEDULE_ID, seatId);
//        return redisTemplate.opsForValue().get(key);
//    }
//
//    private void deleteOwnerKey(Long seatId) {
//        String key = String.format("seat:hold:owner:%d:%d", SCHEDULE_ID, seatId);
//        redisTemplate.delete(key);
//    }
//
//    private void deleteRedisKeysLike(String pattern) {
//        var keys = redisTemplate.keys(pattern);
//        if (keys != null && !keys.isEmpty()) {
//            redisTemplate.delete(keys);
//        }
//    }
//}
