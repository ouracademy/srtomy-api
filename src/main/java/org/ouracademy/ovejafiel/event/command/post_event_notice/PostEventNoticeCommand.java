package org.ouracademy.ovejafiel.event.command.post_event_notice;

import org.ouracademy.ovejafiel.event.domain.EventId;

/** An announcement the organizers write for the page of the event. */
public record PostEventNoticeCommand(EventId eventId, String message) {}
