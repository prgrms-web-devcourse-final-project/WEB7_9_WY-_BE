package back.kalender.global.initData.chat;

import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.repository.PartyRepository;
import back.kalender.domain.user.entity.User;
import back.kalender.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"local", "dev"})
@Order(8)  // Party가 먼저 생성되어야 함
@RequiredArgsConstructor
@Slf4j
public class ChatMessageBaseInitData implements ApplicationRunner {

    private final ChatMessageRepository chatMessageRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (chatMessageRepository.count() > 0) {
            log.info("ChatMessage base data already initialized");
            return;
        }
        createChatMessages();
    }

    private void createChatMessages() {
        List<Party> parties = partyRepository.findAll();
        List<User> users = userRepository.findAll();

        if (parties.isEmpty() || users.isEmpty()) {
            log.warn("Not enough parties or users to create chat messages");
            return;
        }

        List<String> sampleMessages = List.of(
                "안녕하세요! 잘 부탁드립니다 😊",
                "공연 너무 기대되네요 ㅎㅎ",
                "몇 시쯤 출발하면 될까요?",
                "저도 처음인데 같이 가요~",
                "공연장 근처 맛집 아시는 분?",
                "다들 응원봉 챙기셨나요?",
                "출발 시간 10분 전에 도착할게요!",
                "혹시 지각하시는 분 있으신가요?",
                "다들 몇 번째 콘서트이세요?",
                "오늘 날씨 좋네요!",
                "저 아직 티켓 수령 못했어요 ㅠㅠ",
                "공연 몇 시에 시작이에요?",
                "주차장 정보 아시는 분?",
                "혹시 포토카드 교환하실 분?",
                "MD 사시는 분 계신가요?",
                "공연 후 사인회 가시나요?"
        );

        int messageCount = 0;
        for (Party party : parties) {
            // 리더 JOIN 메시지
            chatMessageRepository.save(
                    ChatMessage.createJoinMessage(party.getId(), party.getLeaderId())
            );
            messageCount++;

            // 리더의 환영 메시지
            chatMessageRepository.save(
                    ChatMessage.createChatMessage(
                            party.getId(),
                            party.getLeaderId(),
                            "파티에 오신 것을 환영합니다! 같이 즐거운 시간 보내요 😊"
                    )
            );
            messageCount++;

            // 랜덤 채팅 메시지 (3-7개)
            int msgCount = 3 + (int)(Math.random() * 5);
            for (int i = 0; i < msgCount; i++) {
                User sender = users.get((int)(Math.random() * users.size()));
                String message = sampleMessages.get((int)(Math.random() * sampleMessages.size()));

                chatMessageRepository.save(
                        ChatMessage.createChatMessage(party.getId(), sender.getId(), message)
                );
                messageCount++;
            }
        }

        log.info("ChatMessage base data initialized: {} messages", messageCount);
    }
}