> Migrated from `adrs/ROLES.md`. Informal design notes, not a completed spec — run this
> through the `spec-driven-development` skill's Specify phase if this module (roles /
> accountabilities) gets touched again.

## Roles Architect Design Record (ADR)

1. roles predefinidos + roles
2. persona + roles

GET organization/location/1/roles
=> roles predefinidos + roles

POST organization/{id}/location
PUT organization/location/{id}

class Request {
// ..
List<String> roles;
}

syncRoles() {
location.roles.name not in roles => delete those not in // A - B
new roles not in location.roles => add // B - A

    // check if trying delete a role that is used then what should we do?
    // 1. remove role from that person

    // note delete can be hard or soft
    // if soft store => deleted = true or end_date = now()

}

POST people
PUT people/{id}

class Request {
// ..
List<String> roles;
}

## syncRoles();

model
1st option:
class RoleType {
String name;
boolean isPredefined;
Long locationId;
}

class Role {
Person person;
RoleType roleType;
}

class Person {
String firstName;
// ..
List<Role> roles;
}

2nd option:
class AccountabilityType {
String name;
boolean isPredefined;

    Long locationId; // or Party party;

}

class Accountability {
// ...
AccountabilityType type;
}

class Person {
String firstName;
// ..
}
