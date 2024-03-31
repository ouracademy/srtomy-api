package org.ouracademy.srtomy.organization;

import org.ouracademy.srtomy.person.Party;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Entity
@Getter
public class Organization extends Party {

    @Enumerated(EnumType.STRING)
    Type type;

    enum Type {
        CHURCH, LOCATION, GROUP, MINISTRY
    }

    Organization() {
        // jpa only
    }

    Organization(String name) {
        this.name = name;
    }
}
