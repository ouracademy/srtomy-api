package org.ouracademy.srtomy.user.command.link_user_to_person;

/**
 * Fails with a {@code DomainValidationException} when the person already has a user or the user
 * already has a person in that church.
 */
public interface LinkUserToPersonCommandHandler {
  LinkUserToPersonResult handle(LinkUserToPersonCommand command);
}
