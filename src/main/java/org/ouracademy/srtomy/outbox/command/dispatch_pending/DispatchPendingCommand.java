package org.ouracademy.srtomy.outbox.command.dispatch_pending;

import java.time.Duration;

/** Schedules at most {@code limit} messages nothing has touched for {@code untouchedFor}. */
public record DispatchPendingCommand(Duration untouchedFor, int limit) {}
