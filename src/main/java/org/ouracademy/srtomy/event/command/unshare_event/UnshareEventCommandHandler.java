package org.ouracademy.srtomy.event.command.unshare_event;

/**
 * Stops sharing the event with a person. Fails with {@code NoSuchElementException} (404) when the
 * share does not exist or belongs to another event.
 */
public interface UnshareEventCommandHandler {
  UnshareEventResult handle(UnshareEventCommand command);
}
