package org.ouracademy.srtomy.person;

import java.util.List;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class Accountability extends DomainObject {

    @ManyToOne(optional = false)
    Party child;

    @ManyToOne(optional = false)
    Party parent;

    @Enumerated(EnumType.STRING)
    Type type;

    enum Type {
        // Person and Org (like location, group or ministry)
        VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER,
        // Org and Org
        IS_PART_OF
    }

    public static List<Type> MEMBER_ACCOUNTABILITIES = List.of(Type.VISITOR, Type.MEMBERSHIP_IN_PROGRESS, Type.MEMBER);

    Accountability() {
        // jpa only
    }

    Accountability(Party child, Party parent, Type type) {
        this.child = child;
        this.parent = parent;
        this.type = type;
    }

}
