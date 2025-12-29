package back.kalender.global.initData.chat;

import back.kalender.domain.chat.entity.ChatMessage;
import back.kalender.domain.chat.repository.ChatMessageRepository;
import back.kalender.domain.party.entity.Party;
import back.kalender.domain.party.entity.PartyMember;
import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.repository.PartyMemberRepository;
import back.kalender.domain.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"prod", "dev"})
@Order(8)
@RequiredArgsConstructor
@Slf4j
public class ChatMessageBaseInitData implements ApplicationRunner {

    private final ChatMessageRepository chatMessageRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long totalMessages = chatMessageRepository.count();
        long totalParties = partyRepository.count();

        if (totalParties > 0 && totalMessages / totalParties > 2) {
            log.info("ChatMessage base data already initialized");
            return;
        }

        createChatMessages();
    }

    private void createChatMessages() {
        List<Party> parties = partyRepository.findAll();

        if (parties.isEmpty()) {
            log.warn("No parties found to create chat messages");
            return;
        }

        int messageCount = 0;

        for (Party party : parties) {
            // COMPLETED와 CANCELLED 상태의 파티는 채팅방이 보이지 않으므로 스킵
            if (party.getStatus() == PartyStatus.COMPLETED || party.getStatus() == PartyStatus.CANCELLED) {
                continue;
            }

            List<PartyMember> members = partyMemberRepository.findActiveMembers(party.getId());

            if (members.isEmpty()) {
                continue;
            }

            int msgCount = getMessageCountByStatus(party.getStatus());

            chatMessageRepository.save(
                    ChatMessage.createChatMessage(
                            party.getId(),
                            party.getLeaderId(),
                            getWelcomeMessage(party.getStatus())
                    )
            );
            messageCount++;

            List<String> sampleMessages = getSampleMessagesByStatus(party.getStatus());

            for (int i = 0; i < msgCount; i++) {
                PartyMember randomMember = members.get((int)(Math.random() * members.size()));
                String message = sampleMessages.get((int)(Math.random() * sampleMessages.size()));

                chatMessageRepository.save(
                        ChatMessage.createChatMessage(
                                party.getId(),
                                randomMember.getUserId(),
                                message
                        )
                );
                messageCount++;
            }
        }

        log.info("=".repeat(60));
        log.info("ChatMessage base data initialized: {} chat messages", messageCount);
        log.info("(Only RECRUITING and CLOSED parties have chat messages)");
        log.info("(JOIN messages were created in PartyBaseInitData and PartyApplicationBaseInitData)");
        log.info("=".repeat(60));
    }

    private int getMessageCountByStatus(PartyStatus status) {
        return switch (status) {
            case RECRUITING -> 5 + (int)(Math.random() * 8);
            case CLOSED -> 8 + (int)(Math.random() * 7);
            default -> 0;
        };
    }

    private String getWelcomeMessage(PartyStatus status) {
        return switch (status) {
            case RECRUITING -> "파티에 오신 것을 환영합니다! 같이 즐거운 시간 보내요 😊";
            case CLOSED -> "파티 정원이 마감되었습니다! 모두 잘 부탁드려요 🎉";
            default -> "";
        };
    }

    private List<String> getSampleMessagesByStatus(PartyStatus status) {
        List<String> messages = new ArrayList<>();

        List<String> commonMessages = List.of(
                "안녕하세요! 잘 부탁드립니다 ^^",
                "반가워요~",
                "처음 뵙겠습니다!",
                "잘 부탁드려요 😊",
                "기대돼요!",
                "같이 가요!",
                "안녕하세요 ㅎㅎ",
                "만나서 반가워요",
                "좋은 시간 되길 바래요"
        );
        messages.addAll(commonMessages);

        switch (status) {
            case RECRUITING -> messages.addAll(List.of(
                    "아직 자리 있나요?",
                    "몇 명이나 모였어요?",
                    "공연 너무 기대되네요 ㅎㅎ",
                    "어디서 만날까요?",
                    "몇 시쯤 출발하면 될까요?",
                    "저도 처음인데 같이 가요~",
                    "공연장 근처 맛집 아시는 분?",
                    "다들 응원봉 챙기셨나요?",
                    "혹시 MD 구매하시나요?",
                    "같은 구역이신 분 계신가요?",
                    "주차 정보 아시는 분?",
                    "교통편이 어떻게 되나요?",
                    "출발 시간 확정됐나요?",
                    "좌석이 어디세요?",
                    "공연장 처음이에요!",
                    "티켓은 다들 받으셨나요?",
                    "현장 수령이신 분?",
                    "예매처가 어디세요?",
                    "같이 가서 좋네요!",
                    "신나요 ㅎㅎ"
            ));

            case CLOSED -> messages.addAll(List.of(
                    "드디어 출발이네요!",
                    "다들 준비되셨나요?",
                    "출발 시간 10분 전에 도착할게요!",
                    "혹시 지각하시는 분 있으신가요?",
                    "다들 몇 번째 콘서트이세요?",
                    "오늘 날씨 좋네요!",
                    "응원봉 꼭 챙기세요~",
                    "간식 챙겨왔어요",
                    "물 챙기세요!",
                    "손난로 가져올걸 그랬어요",
                    "화장실 미리 다녀오세요",
                    "지갑 꼭 챙기세요!",
                    "휴대폰 충전은 하셨나요?",
                    "보조배터리 있어요",
                    "우산 가져오세요",
                    "옷 따뜻하게 입으세요",
                    "다들 연락처 공유할까요?",
                    "단톡방 만들까요?",
                    "도착하면 연락주세요",
                    "기대됩니다!"
            ));
        }

        return messages;
    }
}