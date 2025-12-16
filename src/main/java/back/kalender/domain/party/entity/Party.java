package back.kalender.domain.party.entity;

import back.kalender.global.common.Enum.Gender;
import back.kalender.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Party extends BaseEntity {

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false)
    private PartyType partyType;

    @Column(name = "party_name", nullable = false, length = 255)
    private String partyName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "departure_location", nullable = false, length = 255)
    private String departureLocation;

    @Column(name = "arrival_location", nullable = false, length = 255)
    private String arrivalLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private TransportType transportType;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "current_members", nullable = false)
    private Integer currentMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_gender", nullable = false)
    private Gender preferredGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_age", nullable = false)
    private PreferredAge preferredAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PartyStatus status;

    @Builder
    public Party(Long scheduleId, Long leaderId, PartyType partyType, String partyName,
                 String description, String departureLocation, String arrivalLocation,
                 TransportType transportType, Integer maxMembers, Gender preferredGender,
                 PreferredAge preferredAge) {
        this.scheduleId = scheduleId;
        this.leaderId = leaderId;
        this.partyType = partyType;
        this.partyName = partyName;
        this.description = description;
        this.departureLocation = departureLocation;
        this.arrivalLocation = arrivalLocation;
        this.transportType = transportType;
        this.maxMembers = maxMembers;
        this.currentMembers = 1; // 파티장 포함
        this.preferredGender = preferredGender;
        this.preferredAge = preferredAge;
        this.status = PartyStatus.RECRUITING; // 기본값: 모집중
    }

    public Party update(String partyName, String description, String departureLocation,
                       String arrivalLocation, TransportType transportType, Integer maxMembers,
                       Gender preferredGender, PreferredAge preferredAge) {
        if (partyName != null) {
            this.partyName = partyName;
        }
        if (description != null) {
            this.description = description;
        }
        if (departureLocation != null) {
            this.departureLocation = departureLocation;
        }
        if (arrivalLocation != null) {
            this.arrivalLocation = arrivalLocation;
        }
        if (transportType != null) {
            this.transportType = transportType;
        }
        if (maxMembers != null) {
            this.maxMembers = maxMembers;
        }
        if (preferredGender != null) {
            this.preferredGender = preferredGender;
        }
        if (preferredAge != null) {
            this.preferredAge = preferredAge;
        }
        return this;
    }


    public void changeStatus(PartyStatus status) {
        this.status = status;
    }

    public void incrementCurrentMembers() {
        this.currentMembers++;
    }

    public void decrementCurrentMembers() {
        if (this.currentMembers > 1) {
            this.currentMembers--;
        }
    }

    public boolean isLeader(Long userId) {
        return this.leaderId.equals(userId);
    }

    public boolean isFull() {
        return this.currentMembers >= this.maxMembers;
    }

    public boolean isRecruiting() {
        return this.status == PartyStatus.RECRUITING;
    }

    public void assignChatRoom(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}