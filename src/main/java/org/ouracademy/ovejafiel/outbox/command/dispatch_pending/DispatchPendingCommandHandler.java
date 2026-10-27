package org.ouracademy.ovejafiel.outbox.command.dispatch_pending;

/**
 * The daily sweep: schedules the messages that were saved without a task (a bulk of them) or whose
 * task was lost. Scheduling a task that exists is harmless.
 */
public interface DispatchPendingCommandHandler {
  DispatchPendingResult handle(DispatchPendingCommand command);
}
