package org.ouracademy.ovejafiel.event.controller.dto;

/**
 * The email is sent soon after the share is saved: {@code emailStatus} is PENDING until it leaves
 * (it is retried on its own), SENT, or FAILED when it was given up. The share applies anyway.
 * {@code emailSent} is true only for SENT.
 */
public record ShareEventResponse(
    EventSharingResponse.ShareResponse share, boolean emailSent, String emailStatus) {}
