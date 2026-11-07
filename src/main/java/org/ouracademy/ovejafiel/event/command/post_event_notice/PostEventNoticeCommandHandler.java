package org.ouracademy.ovejafiel.event.command.post_event_notice;

public interface PostEventNoticeCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the event does not exist
   * @throws org.ouracademy.ovejafiel.shared.domain.DomainValidationException for an empty or too
   *     long message
   */
  PostEventNoticeResult handle(PostEventNoticeCommand command);
}
