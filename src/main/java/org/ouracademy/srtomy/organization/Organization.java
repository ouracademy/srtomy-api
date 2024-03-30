package org.ouracademy.srtomy.organization;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Entity
@Getter
public class Organization extends DomainObject {

    String name;

    @Column(columnDefinition = "text")
    String description;

    String address;

    String phoneNumber;

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
