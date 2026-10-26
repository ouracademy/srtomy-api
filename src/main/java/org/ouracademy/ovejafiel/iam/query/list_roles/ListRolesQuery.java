package org.ouracademy.ovejafiel.iam.query.list_roles;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The roles a church can grant: the predefined ones plus its own live custom roles. */
public record ListRolesQuery(PartyId churchId) {}
