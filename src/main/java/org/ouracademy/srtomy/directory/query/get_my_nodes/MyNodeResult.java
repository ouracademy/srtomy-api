package org.ouracademy.srtomy.directory.query.get_my_nodes;

import java.util.List;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * A node where the user's own person is a member or a leader. {@code positions} are the names of
 * the positions (cargos) the person holds on it.
 */
public record MyNodeResult(
    PartyId id,
    String name,
    Organization.Type type,
    PartyId churchId,
    String churchName,
    boolean leader,
    List<String> positions) {}
