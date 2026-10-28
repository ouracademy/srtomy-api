package org.ouracademy.ovejafiel.event.controller.dto;

/** Whether the event takes sign-ups right now: false closes the registration, true reopens it. */
public record RegistrationOpenRequest(boolean open) {}
