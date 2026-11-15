package org.ouracademy.ovejafiel.event.controller.dto;

/**
 * Whether the event takes sign-ups right now: false closes the registration, true reopens it.
 *
 * @param open false closes the sign-ups, true opens them again.
 */
public record RegistrationOpenRequest(boolean open) {}
