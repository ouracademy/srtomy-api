package org.ouracademy.srtomy.event.query.authorize_event;

import org.ouracademy.srtomy.event.domain.AccessLevel;

/** What the user can do with the event (at least what was asked). */
public record AuthorizeEventResult(AccessLevel level) {}
