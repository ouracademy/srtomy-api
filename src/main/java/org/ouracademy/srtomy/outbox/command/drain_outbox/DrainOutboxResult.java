package org.ouracademy.srtomy.outbox.command.drain_outbox;

/** What happened to the messages attempted: done, left for a later retry, or given up. */
public record DrainOutboxResult(int sent, int retrying, int failed) {}
