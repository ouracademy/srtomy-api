package org.ouracademy.srtomy.event.command.share_event;

import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.ShareRole;

/** Shares the event with {@code email}; {@code subject} is the user who shares. */
public record ShareEventCommand(String subject, EventId eventId, String email, ShareRole role) {}
