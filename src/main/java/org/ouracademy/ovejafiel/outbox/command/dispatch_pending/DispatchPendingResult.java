package org.ouracademy.ovejafiel.outbox.command.dispatch_pending;

public record DispatchPendingResult(int scheduled, int couldNotSchedule) {}
