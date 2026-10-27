package org.ouracademy.ovejafiel.event.command.cancel_registration_by_code;

/** Cancels the sign-up whose {@code cancelCode} the person kept. */
public record CancelRegistrationByCodeCommand(String cancelCode) {}
