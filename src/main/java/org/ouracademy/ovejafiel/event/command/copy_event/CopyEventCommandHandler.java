package org.ouracademy.ovejafiel.event.command.copy_event;

public interface CopyEventCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the event does not exist
   * @throws org.ouracademy.ovejafiel.shared.domain.DomainValidationException for a sub-event or an
   *     event of a series, which are not copied on their own
   */
  CopyEventResult handle(CopyEventCommand command);
}
