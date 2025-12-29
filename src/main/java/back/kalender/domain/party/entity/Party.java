package back.kalender.domain.party.entity;

import back.kalender.domain.party.enums.PartyStatus;
import back.kalender.domain.party.enums.PartyType;
import back.kalender.domain.party.enums.PreferredAge;
import back.kalender.domain.party.enums.TransportType;
import back.kalender.global.common.enums.Gender;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false)
    private PartyType partyType;

    @Column(name = "party_name", nullable = false, length = 100)
    private String partyName;

    @Column(name = "description")
    private String description;

    @Column(name = "departure_location", nullable = false)
    private String departureLocation;

    @Column(name = "arrival_location", nullable = false)
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
        this.currentMembers = 1;
        this.preferredGender = preferredGender;
        this.preferredAge = preferredAge;
        this.status = PartyStatus.RECRUITING;
    }

    public Party update(String partyName, String description, String departureLocation,
                        String arrivalLocation, TransportType transportType, Integer maxMembers,
                        Gender preferredGender, PreferredAge preferredAge) {
        this.partyName = partyName != null ? partyName : this.partyName;
        this.description = description != null ? description : this.description;
        this.departureLocation = departureLocation != null ? departureLocation : this.departureLocation;
        this.arrivalLocation = arrivalLocation != null ? arrivalLocation : this.arrivalLocation;
        this.transportType = transportType != null ? transportType : this.transportType;
        this.maxMembers = maxMembers != null ? maxMembers : this.maxMembers;
        this.preferredGender = preferredGender != null ? preferredGender : this.preferredGender;
        this.preferredAge = preferredAge != null ? preferredAge : this.preferredAge;
        return this;
    }

    public void changeStatus(PartyStatus status) {
        this.status = status;
    }

    public void incrementCurrentMembers() {
        this.currentMembers++;
        if (isFull() && this.status == PartyStatus.RECRUITING) {
            this.status = PartyStatus.CLOSED;
        }
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
}