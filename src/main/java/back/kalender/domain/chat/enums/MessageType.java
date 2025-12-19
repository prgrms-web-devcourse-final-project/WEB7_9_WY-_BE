package back.kalender.domain.chat.enums;

public enum MessageType {
    CHAT("일반 채팅"),
    JOIN("입장"),
    LEAVE("퇴장"),
    KICK("강퇴");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
