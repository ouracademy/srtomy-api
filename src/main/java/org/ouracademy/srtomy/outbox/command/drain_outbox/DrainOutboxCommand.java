package org.ouracademy.srtomy.outbox.command.drain_outbox;

/** {@code limit}: the most messages to attempt in this call. */
public record DrainOutboxCommand(int limit) {}
