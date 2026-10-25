package org.ouracademy.srtomy.event.command.register_for_event;

/** {@code alreadyRegistered} when that email had signed up before; nothing new is saved. */
public record RegisterForEventResult(boolean alreadyRegistered) {}
