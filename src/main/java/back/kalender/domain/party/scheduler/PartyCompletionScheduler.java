package back.kalender.domain.party.scheduler;

import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.schedule.entity.Schedule;
import back.kalender.domain.schedule.repository.ScheduleRepository;
import back.kalender.domain.chat.entity.ChatRoom;
import back.kalender.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파티 자동 완료 처리 스케줄러
 * 매일 23:59분에 실행되어 스케줄 종료 후 1주일이 지난 파티를 COMPLETED 상태로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartyCompletionScheduler {

    private final PartyRepository partyRepository;
    private final ScheduleRepository scheduleRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 매일 한국 시간 23:59분에 실행
     * cron: "초 분 시 일 월 요일"
     * 0 59 23 * * * = 매일 23시 59분 0초
     */
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Seoul")
    @Transactional
    public void completeExpiredParties() {
        log.info("파티 자동 완료 스케줄러 시작");

        try {
            // 1. RECRUITING 또는 CLOSED 상태인 파티 조회
            List<Party> activeParties = partyRepository.findByStatusIn(
                    List.of(PartyStatus.RECRUITING, PartyStatus.CLOSED)
            );

            log.info("대상 파티 개수: {}", activeParties.size());

            if (activeParties.isEmpty()) {
                log.info("완료 처리할 파티가 없습니다.");
                return;
            }

            // 2. 각 파티의 스케줄 확인 후 1주일 경과 여부 체크
            int completedCount = 0;
            LocalDateTime now = LocalDateTime.now();

            for (Party party : activeParties) {
                Schedule schedule = scheduleRepository.findById(party.getScheduleId())
                        .orElse(null);

                if (schedule == null) {
                    log.warn("파티 ID {}의 스케줄을 찾을 수 없습니다. (scheduleId: {})",
                            party.getId(), party.getScheduleId());
                    continue;
                }

                // 3. scheduleTime으로부터 1주일(7일) 경과 여부 확인
                if (schedule.getScheduleTime().plusWeeks(1).isBefore(now)) {

                    // 4. 파티 상태를 COMPLETED로 변경
                    party.updateStatus(PartyStatus.COMPLETED);
                    partyRepository.save(party);

                    // 5. 연결된 채팅방도 COMPLETED 상태로 변경
                    ChatRoom chatRoom = chatRoomRepository.findByPartyId(party.getId())
                            .orElse(null);

                    if (chatRoom != null) {
                        chatRoom.deactivate();
                        chatRoomRepository.save(chatRoom);
                        log.info("파티 ID {}, 채팅방 ID {} 완료 처리 완료",
                                party.getId(), chatRoom.getId());
                    }

                    completedCount++;

                    log.info("파티 완료 처리: 파티명={}, 파티ID={}, 스케줄시간={}, 현재시간={}",
                            party.getPartyName(),
                            party.getId(),
                            schedule.getScheduleTime(),
                            now);
                }
            }

            log.info("파티 자동 완료 스케줄러 종료 (완료 처리: {}개)", completedCount);

        } catch (Exception e) {
            log.error("파티 자동 완료 스케줄러 실행 중 오류 발생", e);
        }
    }
}