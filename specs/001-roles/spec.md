# Feature Specification: Roles & Accountabilities

**Feature Branch**: `001-roles`

**Status**: Draft — informal notes, not a completed spec

**Migrated from**: `adrs/ROLES.md` → `SPEC-roles.md` (root) → here, to follow spec-kit's
`specs/<NNN-feature-name>/spec.md` convention. Run this through `/speckit-specify` and
`/speckit-plan` properly if the roles/accountabilities module gets touched again instead
of continuing to hand-edit this file.

---

## Roles Architect Design Record (original notes, verbatim)

1. roles predefinidos + roles
2. persona + roles

```
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
```

## syncRoles() — model options

**1st option:**

```
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
```

**2nd option:**

```
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
```
