package org.ouracademy.srtomy.event.controller.dto;

/** {@code alreadyRegistered} when that email had signed up before; nothing new was saved. */
public record RegistrationOutcomeResponse(boolean alreadyRegistered) {}
