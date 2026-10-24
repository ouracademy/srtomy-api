package org.ouracademy.srtomy.event.query.authorize_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.event.domain.AccessLevel;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.query.EventAccess;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

class AuthorizeEventQueryHandlerImplTests {

  EventRepository repository = mock(EventRepository.class);
  EventAccess access = mock(EventAccess.class);
  AuthorizeEventQueryHandlerImpl handler = new AuthorizeEventQueryHandlerImpl(repository, access);

  EventId id = EventId.generate();
  org.ouracademy.srtomy.event.domain.Event event =
      singleEvent(
          id,
          "Retiro",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void passesWhenTheUserCanDoAtLeastWhatIsAsked() {
    when(repository.findById(id)).thenReturn(Optional.of(event));
    when(access.levelOn("me", event)).thenReturn(AccessLevel.EDITOR);

    assertThat(handler.handle(new AuthorizeEventQuery("me", id, AccessLevel.VIEWER)).level())
        .isEqualTo(AccessLevel.EDITOR);
    assertThat(handler.handle(new AuthorizeEventQuery("me", id, AccessLevel.EDITOR)).level())
        .isEqualTo(AccessLevel.EDITOR);
  }

  @Test
  void deniesWhenTheUserCanDoLess() {
    when(repository.findById(id)).thenReturn(Optional.of(event));
    when(access.levelOn("me", event)).thenReturn(AccessLevel.EDITOR);

    assertThatThrownBy(() -> handler.handle(new AuthorizeEventQuery("me", id, AccessLevel.MANAGER)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anUnknownEventIsNotFoundBeforeAnyPermissionCheck() {
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new AuthorizeEventQuery("me", id, AccessLevel.VIEWER)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
