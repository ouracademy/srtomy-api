package org.ouracademy.ovejafiel.event.command.renew_calendar_feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.event.domain.CalendarFeed;
import org.ouracademy.ovejafiel.event.repository.CalendarFeedRepository;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;

@ExtendWith(MockitoExtension.class)
class RenewCalendarFeedCommandHandlerImplTests {

  @Mock FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  @Mock CalendarFeedRepository calendarFeedRepository;

  RenewCalendarFeedCommandHandlerImpl handler;
  UserResult user = new UserResult(UserId.generate(), "auth0|ana", "ana@x.org", true);

  @BeforeEach
  void setUp() {
    handler =
        new RenewCalendarFeedCommandHandlerImpl(
            findUserBySubjectQueryHandler, calendarFeedRepository);
  }

  @Test
  void changesTheTokenOfTheExistingFeed() {
    var feed = new CalendarFeed(user.id());
    var old = feed.getToken();
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|ana")))
        .thenReturn(Optional.of(user));
    when(calendarFeedRepository.findById(user.id())).thenReturn(Optional.of(feed));
    when(calendarFeedRepository.save(feed)).thenReturn(feed);

    var result = handler.handle(new RenewCalendarFeedCommand("auth0|ana"));

    assertThat(result.token()).isNotEqualTo(old).isEqualTo(feed.getToken());
  }

  @Test
  void createsTheFeedWhenTheUserHadNone() {
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|ana")))
        .thenReturn(Optional.of(user));
    when(calendarFeedRepository.findById(user.id())).thenReturn(Optional.empty());
    when(calendarFeedRepository.save(any(CalendarFeed.class))).thenAnswer(c -> c.getArgument(0));

    assertThat(handler.handle(new RenewCalendarFeedCommand("auth0|ana")).token()).hasSize(16);
  }

  @Test
  void failsWhenTheUserDoesNotExist() {
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|x")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new RenewCalendarFeedCommand("auth0|x")))
        .isInstanceOf(NoSuchElementException.class);
  }
}
