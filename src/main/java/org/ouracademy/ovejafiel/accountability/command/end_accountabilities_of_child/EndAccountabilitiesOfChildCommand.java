package org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Closes (keeps as history, without deleting) the open accountabilities where {@code childId} is
 * the child and the parent is one of {@code parentIds}: their membership, state and positions
 * there.
 */
public record EndAccountabilitiesOfChildCommand(PartyId childId, List<PartyId> parentIds) {}
