package org.ouracademy.ovejafiel.event.command.register_for_event;

import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;

/**
 * {@code alreadyRegistered} when that email already had a place; nothing new is saved and the codes
 * are those of the existing sign-up. {@code cancelCode} cancels it without an account and {@code
 * ticketCode} is what the ticket QR carries.
 */
public record RegisterForEventResult(
    boolean alreadyRegistered, RegistrationStatus status, String cancelCode, String ticketCode) {}
