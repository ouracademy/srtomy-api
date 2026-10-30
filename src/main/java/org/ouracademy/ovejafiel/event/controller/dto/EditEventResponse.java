package org.ouracademy.ovejafiel.event.controller.dto;

import java.util.UUID;

/**
 * {@code keptEvents}: occurrences left untouched, as one-off events, because they are past or have
 * sign-ups or attendance. {@code notifiedPeople}: people emailed about a new time.
 */
public record EditEventResponse(UUID id, int keptEvents, int notifiedPeople) {}
