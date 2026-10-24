package org.ouracademy.srtomy.event.query.get_shared_event;

/**
 * The event whose shared link is {@code slug}, as seen by {@code subject} (null: not signed in).
 */
public record GetSharedEventQuery(String slug, String subject) {}
