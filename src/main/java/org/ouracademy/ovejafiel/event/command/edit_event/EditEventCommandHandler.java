package org.ouracademy.ovejafiel.event.command.edit_event;

public interface EditEventCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the event or any of the speakers does not exist
   * @throws org.ouracademy.ovejafiel.shared.domain.DomainValidationException when the node or the
   *     kind (event or course) would change, or the new data makes no sense
   */
  EditEventResult handle(EditEventCommand command);
}
