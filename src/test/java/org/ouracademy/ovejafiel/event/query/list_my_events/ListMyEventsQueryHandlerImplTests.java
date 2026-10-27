package org.ouracademy.ovejafiel.event.query.list_my_events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository.MyEventRow;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;

class ListMyEventsQueryHandlerImplTests {

  FindUserBySubjectQueryHandler users = mock(FindUserBySubjectQueryHandler.class);
  EventRepository repository = mock(EventRepository.class);
  ListMyEventsQueryHandlerImpl handler = new ListMyEventsQueryHandlerImpl(users, repository);

  UserId id = UserId.generate();

  @Test
  void mapsTheRowsOfTheUser() {
    when(users.handle(any())).thenReturn(Optional.of(new UserResult(id, "me", "me@x.org", true)));
    var row = mock(MyEventRow.class);
    when(row.getTitle()).thenReturn("Curso");
    when(row.getStart()).thenReturn(Instant.parse("2030-01-05T15:00:00Z"));
    when(row.getIsACourse()).thenReturn(true);
    when(row.getOrganizationName()).thenReturn("Jóvenes");
    when(row.getRegistered()).thenReturn(true);
    when(row.getAttended()).thenReturn(false);
    when(row.getSlug()).thenReturn("abc");
    when(repository.findOfUser(id.value(), "me@x.org")).thenReturn(List.of(row));

    var events = handler.handle(new ListMyEventsQuery("me"));

    assertThat(events).hasSize(1);
    var event = events.get(0);
    assertThat(event.title()).isEqualTo("Curso");
    assertThat(event.start().toInstant()).isEqualTo(Instant.parse("2030-01-05T15:00:00Z"));
    assertThat(event.isACourse()).isTrue();
    assertThat(event.registered()).isTrue();
    assertThat(event.attended()).isFalse();
    assertThat(event.slug()).isEqualTo("abc");
  }

  @Test
  void anUnverifiedEmailIsNotUsedToFindSignUps() {
    when(users.handle(any())).thenReturn(Optional.of(new UserResult(id, "me", "me@x.org", false)));
    when(repository.findOfUser(any(), any())).thenReturn(List.of());

    assertThat(handler.handle(new ListMyEventsQuery("me"))).isEmpty();
    verify(repository).findOfUser(any(), isNull());
  }

  @Test
  void anUnknownUserHasNoEvents() {
    when(users.handle(any())).thenReturn(Optional.empty());

    assertThat(handler.handle(new ListMyEventsQuery("nobody"))).isEmpty();
  }
}
