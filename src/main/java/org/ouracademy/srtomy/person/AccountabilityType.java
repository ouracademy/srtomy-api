package org.ouracademy.srtomy.person;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
@SQLDelete(sql = "UPDATE accountability_type SET deleted_time = now() WHERE id=?")
@SQLRestriction("deleted_time is not null")
public class AccountabilityType extends DomainObject {

    String name;

    @ManyToOne
    Party party;

    LocalDateTime deletedTime;

    AccountabilityType() {
        // jpa only
    }

    public AccountabilityType(String name, Party party) {
        this.name = name;
        this.party = party;
    }

    boolean isPredefined() {
        return party == null;
    }
}
