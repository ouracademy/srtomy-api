package org.ouracademy.srtomy.event.command.register_for_event;

/**
 * Signs up for the event whose shared link is {@code slug}; {@code subject} is null if anonymous.
 */
public record RegisterForEventCommand(
    String slug,
    String subject,
    String firstName,
    String lastName,
    String email,
    String phoneNumber) {}
