package org.ouracademy.srtomy.accountability.command.sync_person_positions;

public interface SyncPersonPositionsCommandHandler {
  SyncPersonPositionsResult handle(SyncPersonPositionsCommand command);
}
