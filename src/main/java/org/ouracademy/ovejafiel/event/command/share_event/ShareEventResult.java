package org.ouracademy.ovejafiel.event.command.share_event;

import org.ouracademy.ovejafiel.event.query.EventSharingResult;

/** {@code emailSent} is false when the provider failed; the share exists and applies anyway. */
public record ShareEventResult(EventSharingResult.Share share, boolean emailSent) {}
