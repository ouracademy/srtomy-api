package org.ouracademy.srtomy.person;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class AccountabilityType extends DomainObject {

    String name;

    @ManyToOne
    Party party;

    boolean isPredefined() {
        return party == null;
    }
}
