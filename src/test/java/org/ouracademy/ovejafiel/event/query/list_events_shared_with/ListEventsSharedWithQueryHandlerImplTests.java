package org.ouracademy.ovejafiel.event.query.list_events_shared_with;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;

class ListEventsSharedWithQueryHandlerImplTests {

  EventAccess access = mock(EventAccess.class);
  EventShareRepository shares = mock(EventShareRepository.class);
  ListEventsSharedWithQueryHandlerImpl handler =
      new ListEventsSharedWithQueryHandlerImpl(access, shares);

  Organization group = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP);

  @Test
  void listsTheEventsSharedWithTheVerifiedEmailSoonestFirst() {
    when(access.verifiedUser("me"))
        .thenReturn(new UserResult(UserId.generate(), "me", "ana@x.org", true));
    var late = singleEvent(EventId.generate(), "Tarde", group, List.of());
    var soon = singleEvent(EventId.generate(), "Pronto", group, List.of());
    org.springframework.test.util.ReflectionTestUtils.setField(
        late, "start", ZonedDateTime.parse("2027-01-01T10:00:00Z"));
    org.springframework.test.util.ReflectionTestUtils.setField(
        soon, "start", ZonedDateTime.parse("2026-06-01T10:00:00Z"));
    when(shares.findByEmailIgnoreCase("ana@x.org"))
        .thenReturn(
            List.of(
                new EventShare(late, "ana@x.org", ShareRole.VIEWER, null),
                new EventShare(soon, "ana@x.org", ShareRole.EDITOR, null)));

    var result = handler.handle(new ListEventsSharedWithQuery("me"));

    assertThat(result).extracting(SharedWithMeResult::title).containsExactly("Pronto", "Tarde");
    assertThat(result.get(0).role()).isEqualTo(ShareRole.EDITOR);
    assertThat(result.get(0).organizationName()).isEqualTo("Jóvenes");
    assertThat(result.get(0).slug()).isEqualTo(soon.getShareSlug());
  }

  @Test
  void withoutAVerifiedEmailNothingIsListed() {
    when(access.verifiedUser("me")).thenReturn(null);

    assertThat(handler.handle(new ListEventsSharedWithQuery("me"))).isEmpty();
    verifyNoInteractions(shares);
  }
}
