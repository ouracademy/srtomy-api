package org.ouracademy.srtomy.event.command.set_event_sharing;

/**
 * Changes who can open the event through its link and what that allows. Fails with {@code
 * NoSuchElementException} (404) when the event does not exist and with {@code
 * DomainValidationException} (400) when the registrant role is asked for an event that is not a
 * course.
 */
public interface SetEventSharingCommandHandler {
  SetEventSharingResult handle(SetEventSharingCommand command);
}
