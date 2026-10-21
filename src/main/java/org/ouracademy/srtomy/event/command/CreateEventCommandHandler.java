package org.ouracademy.srtomy.event.command;

public interface CreateEventCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the organization or any of the speakers does not
   *     exist
   */
  CreateEventResult handle(CreateEventCommand command);
}
