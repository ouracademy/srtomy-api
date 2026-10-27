package org.ouracademy.ovejafiel.event.command.create_event;

public interface CreateEventCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the organization or any of the speakers does not
   *     exist
   */
  CreateEventResult handle(CreateEventCommand command);
}
