package org.ouracademy.ovejafiel.directory.query.get_my_nodes;

import java.util.List;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * A node where the user's own person is on the list of (belongs to) or holds a position. {@code
 * positions} are the names of the positions (cargos) the person holds on it.
 */
public record MyNodeResult(
    PartyId id,
    String name,
    Organization.Type type,
    PartyId churchId,
    String churchName,
    List<String> positions) {}
