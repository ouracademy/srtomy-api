package org.ouracademy.srtomy.event.query.list_events_shared_with;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.query.EventAccess;
import org.ouracademy.srtomy.event.repository.EventShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListEventsSharedWithQueryHandlerImpl implements ListEventsSharedWithQueryHandler {

  private final EventAccess eventAccess;
  private final EventShareRepository eventShareRepository;

  @Override
  @Transactional(readOnly = true)
  public List<SharedWithMeResult> handle(ListEventsSharedWithQuery query) {
    var user = eventAccess.verifiedUser(query.subject());
    if (user == null) {
      return List.of();
    }
    return eventShareRepository.findByEmailIgnoreCase(user.email()).stream()
        .map(
            share ->
                new SharedWithMeResult(
                    share.getEvent().getTitle(),
                    share.getEvent().getStart(),
                    Boolean.TRUE.equals(share.getEvent().getIsACourse()),
                    share.getEvent().getOrganization().getName(),
                    share.getRole(),
                    share.getEvent().getShareSlug()))
        .sorted(Comparator.comparing(SharedWithMeResult::start))
        .toList();
  }
}
