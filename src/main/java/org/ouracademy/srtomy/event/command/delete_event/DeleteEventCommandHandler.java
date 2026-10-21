package org.ouracademy.srtomy.event.command.delete_event;

public interface DeleteEventCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when no single event has the given id
   */
  DeleteEventResult handle(DeleteEventCommand command);
}
