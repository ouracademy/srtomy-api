package org.ouracademy.ovejafiel.person.command.restore_person;

public interface RestorePersonCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the person or the organization does not exist
   * @throws org.ouracademy.ovejafiel.shared.domain.ConflictException when the person already
   *     belongs to the organization
   */
  RestorePersonResult handle(RestorePersonCommand command);
}
