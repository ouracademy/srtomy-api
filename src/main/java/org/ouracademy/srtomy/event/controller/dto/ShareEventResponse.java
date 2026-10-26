package org.ouracademy.srtomy.event.controller.dto;

/** {@code emailSent} is false while the email waits or retries: the share applies anyway. */
public record ShareEventResponse(EventSharingResponse.ShareResponse share, boolean emailSent) {}
