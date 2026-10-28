package org.ouracademy.ovejafiel.attendance.command.mark_user_present;

import org.ouracademy.ovejafiel.party.domain.PartyId;

public record MarkUserPresentResult(PartyId personId, boolean alreadyMarked) {}
