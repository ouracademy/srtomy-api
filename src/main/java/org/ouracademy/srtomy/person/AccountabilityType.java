package org.ouracademy.srtomy.person;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
@SQLDelete(sql = "UPDATE accountability_type SET deleted_time = now() WHERE id=?")
@SQLRestriction("deleted_time is null")
public class AccountabilityType extends DomainObject {

    String name;

    @ManyToOne
    Party party;

    LocalDateTime deletedTime;

    @Enumerated(EnumType.STRING)
    Category category;

    public enum Category {
        ROLE, MEMBER_PROGRESS_STATE
    }

    AccountabilityType() {
        // jpa only
    }

    public AccountabilityType(String name, Party party, Category category) {
        this.name = name;
        this.party = party;
        this.category = category;
    }

    public boolean isPredefined() {
        return party == null;
    }
}
