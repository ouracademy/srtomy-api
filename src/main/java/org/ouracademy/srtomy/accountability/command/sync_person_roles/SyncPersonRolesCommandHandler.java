package org.ouracademy.srtomy.accountability.command.sync_person_roles;

public interface SyncPersonRolesCommandHandler {
  SyncPersonRolesResult handle(SyncPersonRolesCommand command);
}
