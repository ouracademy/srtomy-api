package org.ouracademy.ovejafiel.attendance.controller.dto;

/** Mark a sign-up as present, or take the mark back. */
public record RegistrationCheckInRequest(boolean checkedIn) {}
