package org.ouracademy.ovejafiel.event.controller.dto;

/** {@code emailSent} is false when the provider failed; the share exists and applies anyway. */
public record ShareEventResponse(EventSharingResponse.ShareResponse share, boolean emailSent) {}
