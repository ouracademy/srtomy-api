package org.ouracademy.ovejafiel.event.command.ensure_calendar_feed;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.CalendarFeed;
import org.ouracademy.ovejafiel.event.repository.CalendarFeedRepository;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EnsureCalendarFeedCommandHandlerImpl implements EnsureCalendarFeedCommandHandler {

  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final CalendarFeedRepository calendarFeedRepository;

  @Override
  @Transactional
  public EnsureCalendarFeedResult handle(EnsureCalendarFeedCommand command) {
    var user =
        findUserBySubjectQueryHandler
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new NoSuchElementException("User not found"));
    var feed =
        calendarFeedRepository
            .findById(user.id())
            .orElseGet(() -> calendarFeedRepository.save(new CalendarFeed(user.id())));
    return new EnsureCalendarFeedResult(feed.getToken());
  }
}
