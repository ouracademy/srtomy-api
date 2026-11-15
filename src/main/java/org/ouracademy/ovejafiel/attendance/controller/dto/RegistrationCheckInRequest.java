package org.ouracademy.ovejafiel.attendance.controller.dto;

/**
 * Mark a sign-up as present, or take the mark back.
 *
 * @param checkedIn true marks the sign-up as present, false takes the mark back.
 */
public record RegistrationCheckInRequest(boolean checkedIn) {}
