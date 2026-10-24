package org.ouracademy.srtomy.event.command.renew_event_link;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RenewEventLinkCommandHandlerImpl implements RenewEventLinkCommandHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional
  public RenewEventLinkResult handle(RenewEventLinkCommand command) {
    var event =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    event.renewShareLink();
    return new RenewEventLinkResult(event.getShareSlug());
  }
}
