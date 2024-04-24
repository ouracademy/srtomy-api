package org.ouracademy.srtomy.person;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
@SQLDelete(sql = "UPDATE accountability SET deleted_time = now() WHERE id=?")
@SQLRestriction("deleted_time is null")
public class Accountability extends DomainObject {

    @ManyToOne(optional = false)
    Party child;

    @ManyToOne(optional = false)
    Party parent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type")
    AccountabilityType type;

    LocalDateTime deletedTime;

    Accountability() {
        // jpa only
    }

    public Accountability(Party child, Party parent, AccountabilityType type) {
        this.child = child;
        this.parent = parent;
        this.type = type;
    }

}
