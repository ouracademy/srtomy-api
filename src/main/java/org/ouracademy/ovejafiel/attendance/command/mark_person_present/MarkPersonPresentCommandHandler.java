package org.ouracademy.ovejafiel.attendance.command.mark_person_present;

/** Fails with {@code NoSuchElementException} (404) when the event or the person does not exist. */
public interface MarkPersonPresentCommandHandler {
  MarkPersonPresentResult handle(MarkPersonPresentCommand command);
}
