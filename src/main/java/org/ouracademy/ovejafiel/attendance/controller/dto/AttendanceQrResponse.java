package org.ouracademy.ovejafiel.attendance.controller.dto;

import java.time.Instant;

/**
 * The code to show as a QR (the page is {@code /a/{token}}); ask for a new one at {@code
 * refreshAt}.
 */
public record AttendanceQrResponse(String token, Instant refreshAt) {}
