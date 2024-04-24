package org.ouracademy.srtomy.person;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class Accountability extends DomainObject {

    @ManyToOne(optional = false)
    Party child;

    @ManyToOne(optional = false)
    Party parent;

    @ManyToOne(optional = false)
    AccountabilityType type;

    Accountability() {
        // jpa only
    }

    public Accountability(Party child, Party parent, AccountabilityType type) {
        this.child = child;
        this.parent = parent;
        this.type = type;
    }

}
