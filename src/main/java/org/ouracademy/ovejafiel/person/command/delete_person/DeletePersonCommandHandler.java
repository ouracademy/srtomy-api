package org.ouracademy.ovejafiel.person.command.delete_person;

public interface DeletePersonCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when {@code organizationId} is not an organization
   */
  DeletePersonResult handle(DeletePersonCommand command);
}
