package org.ouracademy.srtomy.person;

import org.ouracademy.srtomy.domain.DomainObject;
import org.ouracademy.srtomy.organization.Organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

@Entity
public class Accountability extends DomainObject {

    @ManyToOne(optional = false)
    Person person;

    @ManyToOne(optional = false)
    Organization organization;

    @Enumerated(EnumType.STRING)
    Type type;

    enum Type {
        VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER
    }

    Accountability() {
        // jpa only
    }

    Accountability(Person person, Organization organization, Type type) {
        this.person = person;
        this.organization = organization;
        this.type = type;
    }

}
