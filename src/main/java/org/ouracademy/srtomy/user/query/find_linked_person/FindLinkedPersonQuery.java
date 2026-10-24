package org.ouracademy.srtomy.user.query.find_linked_person;

import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;

/** The person that represents {@code userId} in the church {@code churchId}, if any. */
public record FindLinkedPersonQuery(UserId userId, PartyId churchId) {}
