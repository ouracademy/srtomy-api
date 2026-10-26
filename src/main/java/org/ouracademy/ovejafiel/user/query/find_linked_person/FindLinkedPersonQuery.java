package org.ouracademy.ovejafiel.user.query.find_linked_person;

import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/** The person that represents {@code userId} in the church {@code churchId}, if any. */
public record FindLinkedPersonQuery(UserId userId, PartyId churchId) {}
