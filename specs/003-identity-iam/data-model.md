# Data model: 003-identity-iam

Migration: `V1.1.0__identity_iam.sql`.

## Cleanup (no real data exists, confirmed by the product owner)

The migration deletes every domain row, children first:
- `person_event_attendance`, `event_attendance`, `event_speakers`, `event`
- `accountability`
- `accountability_type` rows with category `ROLE` (the global PASTOR/WORSHIP_LEADER/USHER and any custom ones)
- `person`, `organization`, `party` (this removes the `LCDP` seed)

It keeps the system types VISITOR, MEMBERSHIP_IN_PROGRESS, MEMBER, IS_PART_OF and LEADER.

## organization (altered)

| column | type | notes |
| --- | --- | --- |
| parent_id | uuid null → organization(id) | `null` only for CHURCH; indexed |
| website, facebook_url, instagram_url, youtube_url, tiktok_url | varchar(500) null | optional social links |

## accountability_type (altered)

- `category` value `ROLE` is renamed to `POSITION`.
- A position always has `party_id` = its church.
- Uniqueness: partial unique index on `(party_id, lower(name)) WHERE category = 'POSITION' AND deleted_time IS NULL`.

## app_user (new)

| column | type | notes |
| --- | --- | --- |
| id | uuid pk | UUIDv7 |
| auth0_sub | varchar(255) not null unique | JWT `sub` |
| email | varchar(320) null | |
| email_verified | boolean not null default false | |
| person_id | uuid null → person(id) | reserved for phase 005 |
| create_date, last_modified_date | timestamp | auditing |

## iam_role (new)

| column | type | notes |
| --- | --- | --- |
| id | uuid pk | fixed UUIDs for predefined roles |
| name | varchar(100) not null | Spanish display name |
| description | text | |
| church_id | uuid null → organization(id) | null = predefined |
| permissions | text[] not null | values of `Permission` |
| deleted_time | timestamp null | soft delete |
| create_date, last_modified_date | timestamp | |

Seeded predefined roles:

| id | name | permissions |
| --- | --- | --- |
| `0195f0a0-0000-7000-8000-000000000001` | Propietario | all 12 |
| `0195f0a0-0000-7000-8000-000000000002` | Administrador | all 12 |
| `0195f0a0-0000-7000-8000-000000000003` | Líder | org.read, org.update, org.create_child, members.read, members.write, events.read, events.write, attendance.write, iam.read |
| `0195f0a0-0000-7000-8000-000000000004` | Colaborador | org.read, members.read, events.read, attendance.write |
| `0195f0a0-0000-7000-8000-000000000005` | Miembro | org.read, events.read |

Churches and locations cannot be deleted in this phase, so `org.delete` only applies to groups and ministries, and Administrador holds every permission. What separates it from Propietario (it cannot grant or revoke Propietario) arrives with role management in phase 004.

## role_binding (new)

| column | type | notes |
| --- | --- | --- |
| id | uuid pk | |
| user_id | uuid not null → app_user(id) | |
| role_id | uuid not null → iam_role(id) | |
| organization_id | uuid not null → organization(id) | indexed |
| granted_by | uuid null → app_user(id) | null for self-grant on church creation |
| create_date, last_modified_date | timestamp | |
| unique | (user_id, role_id, organization_id) | |

## Permission catalog

`org.read`, `org.update`, `org.create_child`, `org.delete`, `members.read`, `members.write`, `events.read`, `events.write`, `attendance.write`, `positions.manage`, `iam.read`, `iam.manage`.
