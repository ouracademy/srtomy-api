package org.ouracademy.srtomy.outbox.command.dispatch_pending;

public record DispatchPendingResult(int scheduled, int couldNotSchedule) {}
