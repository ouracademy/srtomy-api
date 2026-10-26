package org.ouracademy.srtomy.outbox;

/**
 * Published when a message is enqueued; once its transaction commits, an attempt is made at once.
 */
public record OutboxMessageEnqueued() {}
