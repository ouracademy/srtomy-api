package org.ouracademy.srtomy.outbox.command.cancel_message;

/** {@code cancelled} is false when it was too late (already sent or given up) or unknown. */
public record CancelMessageResult(boolean cancelled) {}
