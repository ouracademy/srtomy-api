package org.ouracademy.srtomy.event.command.edit_event;

public interface EditEventCommandHandler {
  /**
   * @throws org.apache.commons.lang3.NotImplementedException always, for now
   */
  EditEventResult handle(EditEventCommand command);
}
