package org.ouracademy.srtomy.iam.query.list_roles;

import org.ouracademy.srtomy.party.domain.PartyId;

/** The roles a church can grant: the predefined ones plus its own live custom roles. */
public record ListRolesQuery(PartyId churchId) {}
