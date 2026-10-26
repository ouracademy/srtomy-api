package org.ouracademy.ovejafiel.iam.query.get_effective_permissions;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Everything {@code subject} may do on {@code organizationId}, inherited bindings included. */
public record GetEffectivePermissionsQuery(String subject, PartyId organizationId) {}
