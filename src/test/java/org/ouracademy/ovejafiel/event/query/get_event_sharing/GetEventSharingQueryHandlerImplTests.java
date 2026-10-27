package org.ouracademy.ovejafiel.event.query.get_event_sharing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetEventSharingQueryHandlerImplTests {

  EventRepository events = mock(EventRepository.class);
  EventShareRepository shares = mock(EventShareRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  GetEventSharingQueryHandlerImpl handler =
      new GetEventSharingQueryHandlerImpl(events, shares, registrations);

  EventId id = EventId.generate();

  @Test
  void describesHowTheEventIsShared() {
    var event =
        singleEvent(
            id,
            "Curso",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    event.setIsACourse(true);
    event.shareVia(GeneralAccess.CHURCH, LinkRole.REGISTRANT);
    when(events.findById(id)).thenReturn(Optional.of(event));
    when(shares.findByEventIdOrderByCreateDate(id))
        .thenReturn(List.of(new EventShare(event, "ana@x.org", ShareRole.EDITOR, null)));
    when(registrations.countByEventId(id)).thenReturn(3L);

    var result = handler.handle(new GetEventSharingQuery(id)).orElseThrow();

    assertThat(result.generalAccess()).isEqualTo(GeneralAccess.CHURCH);
    assertThat(result.linkRole()).isEqualTo(LinkRole.REGISTRANT);
    assertThat(result.slug()).isEqualTo(event.getShareSlug());
    assertThat(result.isACourse()).isTrue();
    assertThat(result.registrations()).isEqualTo(3);
    assertThat(result.shares()).extracting(s -> s.email()).containsExactly("ana@x.org");
    assertThat(result.shares().get(0).role()).isEqualTo(ShareRole.EDITOR);
  }

  @Test
  void anUnknownEventIsEmpty() {
    when(events.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetEventSharingQuery(id))).isEmpty();
  }
}
