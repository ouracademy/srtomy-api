package org.ouracademy.ovejafiel.event.command.ensure_calendar_feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class EnsureCalendarFeedCommandHandlerImplTests {

  @Mock FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  @Mock CalendarFeedRepository calendarFeedRepository;

  EnsureCalendarFeedCommandHandlerImpl handler;
  UserResult user = new UserResult(UserId.generate(), "auth0|ana", "ana@x.org", true);

  @BeforeEach
  void setUp() {
    handler =
        new EnsureCalendarFeedCommandHandlerImpl(
            findUserBySubjectQueryHandler, calendarFeedRepository);
  }

  @Test
  void createsTheFeedOfAUserWhoHasNone() {
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|ana")))
        .thenReturn(Optional.of(user));
    when(calendarFeedRepository.findById(user.id())).thenReturn(Optional.empty());
    when(calendarFeedRepository.save(any(CalendarFeed.class))).thenAnswer(c -> c.getArgument(0));

    var result = handler.handle(new EnsureCalendarFeedCommand("auth0|ana"));

    assertThat(result.token()).hasSize(16);
  }

  @Test
  void returnsTheTokenOfTheExistingFeedWithoutChangingIt() {
    var feed = new CalendarFeed(user.id());
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|ana")))
        .thenReturn(Optional.of(user));
    when(calendarFeedRepository.findById(user.id())).thenReturn(Optional.of(feed));

    var result = handler.handle(new EnsureCalendarFeedCommand("auth0|ana"));

    assertThat(result.token()).isEqualTo(feed.getToken());
    verify(calendarFeedRepository, never()).save(any());
  }

  @Test
  void failsWhenTheUserDoesNotExist() {
    when(findUserBySubjectQueryHandler.handle(new FindUserBySubjectQuery("auth0|x")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new EnsureCalendarFeedCommand("auth0|x")))
        .isInstanceOf(NoSuchElementException.class);
  }
}
