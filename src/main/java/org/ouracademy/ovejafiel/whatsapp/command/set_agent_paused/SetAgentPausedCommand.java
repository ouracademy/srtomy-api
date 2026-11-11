package org.ouracademy.ovejafiel.whatsapp.command.set_agent_paused;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Pause or resume the assistant of the church of this node. */
public record SetAgentPausedCommand(PartyId organizationId, boolean paused) {}
