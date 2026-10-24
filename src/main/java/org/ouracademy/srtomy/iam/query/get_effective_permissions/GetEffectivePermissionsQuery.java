package org.ouracademy.srtomy.iam.query.get_effective_permissions;

import org.ouracademy.srtomy.party.domain.PartyId;

/** Everything {@code subject} may do on {@code organizationId}, inherited bindings included. */
public record GetEffectivePermissionsQuery(String subject, PartyId organizationId) {}
