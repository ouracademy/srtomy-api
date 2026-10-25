package org.ouracademy.srtomy.event.command.share_event;

/**
 * Shares the event with a person by email and tells them by email. Sharing again with the same
 * email changes the role and resends the message. The share applies to whoever signs in with that
 * verified email, even if they have no account yet.
 */
public interface ShareEventCommandHandler {
  ShareEventResult handle(ShareEventCommand command);
}
