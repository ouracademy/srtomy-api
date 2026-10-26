package org.ouracademy.srtomy.outbox.command.drain_outbox;

public interface DrainOutboxCommandHandler {
  DrainOutboxResult handle(DrainOutboxCommand command);
}
