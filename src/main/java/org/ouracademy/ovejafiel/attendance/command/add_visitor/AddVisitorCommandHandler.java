package org.ouracademy.ovejafiel.attendance.command.add_visitor;

/** Fails with {@code NoSuchElementException} (404) when the event does not exist. */
public interface AddVisitorCommandHandler {
  AddVisitorResult handle(AddVisitorCommand command);
}
