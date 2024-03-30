package org.ouracademy.srtomy.person;

import java.util.List;

import org.ouracademy.srtomy.domain.DomainObject;
import org.ouracademy.srtomy.organization.Organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
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

    public static List<Type> MEMBER_ACCOUNTABILITIES = List.of(Type.VISITOR, Type.MEMBERSHIP_IN_PROGRESS, Type.MEMBER);

    Accountability() {
        // jpa only
    }

    Accountability(Person person, Organization organization, Type type) {
        this.person = person;
        this.organization = organization;
        this.type = type;
    }

}
