package org.ouracademy.srtomy.organization;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class Organization extends DomainObject {

    String name;

    String address;

    String phoneNumber;

    @ManyToOne
    Organization parent;

    Organization() {
        // jpa only
    }

    Organization(String name) {
        this.name = name;
    }
}
