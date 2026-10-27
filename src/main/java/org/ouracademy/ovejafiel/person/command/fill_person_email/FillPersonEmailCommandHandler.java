package org.ouracademy.ovejafiel.person.command.fill_person_email;

public interface FillPersonEmailCommandHandler {

  /**
   * An email that is already there is never overwritten: it may be a different address from the one
   * of the account. Fails with {@code NoSuchElementException} when the person does not exist.
   */
  FillPersonEmailResult handle(FillPersonEmailCommand command);
}
