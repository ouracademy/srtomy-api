package org.ouracademy.srtomy.person.command.create_person;

public interface CreatePersonCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when {@code organizationId} is not an organization
   */
  CreatePersonResult handle(CreatePersonCommand command);
}
