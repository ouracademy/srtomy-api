package org.ouracademy.ovejafiel.event.command.copy_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class CopyEventCommandHandlerImplTests {

  EventRepository repository = mock(EventRepository.class);
  CopyEventCommandHandlerImpl handler = new CopyEventCommandHandlerImpl(repository);

  Event congress =
      singleEvent(
          EventId.generate(),
          "Avivamiento 2030",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void saveReturnsWhatItGets() {
    when(repository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));
    when(repository.findById(congress.getId())).thenReturn(Optional.of(congress));
  }

  private OffsetDateTime oneYearLater() {
    return congress.getStart().plusYears(1).toOffsetDateTime();
  }

  @Test
  void copiesTheEventToTheNewDateWithoutSharingItsAccessOrLink() {
    congress.shareVia(GeneralAccess.CHURCH);
    when(repository.findByParentOrderByStart(congress)).thenReturn(List.of());

    var result =
        handler.handle(new CopyEventCommand(congress.getId(), oneYearLater(), "Avivamiento 2031"));

    var saved = ArgumentCaptor.forClass(Event.class);
    org.mockito.Mockito.verify(repository).save(saved.capture());
    var copy = saved.getValue();
    assertThat(result.id()).isEqualTo(copy.getId());
    assertThat(copy.getId()).isNotEqualTo(congress.getId());
    assertThat(copy.getTitle()).isEqualTo("Avivamiento 2031");
    assertThat(copy.getStart().toInstant()).isEqualTo(oneYearLater().toInstant());
    assertThat(copy.duration()).isEqualTo(congress.duration());
    assertThat(copy.getGeneralAccess()).isEqualTo(GeneralAccess.RESTRICTED);
    assertThat(copy.getShareSlug()).isNotEqualTo(congress.getShareSlug());
    assertThat(result.subEvents()).isZero();
  }

  @Test
  void keepsTheNameWhenNoneIsGiven() {
    when(repository.findByParentOrderByStart(congress)).thenReturn(List.of());

    handler.handle(new CopyEventCommand(congress.getId(), oneYearLater(), " "));

    var saved = ArgumentCaptor.forClass(Event.class);
    org.mockito.Mockito.verify(repository).save(saved.capture());
    assertThat(saved.getValue().getTitle()).isEqualTo("Avivamiento 2030");
  }

  @Test
  void copiesTheProgramKeepingEachSubEventInItsPlaceAndHowItIsEntered() {
    var workshop =
        childOf(congress, EventId.generate(), "Taller", congress.getStart(), congress.getEnd());
    workshop.setRegistrationMode(RegistrationMode.OWN);
    when(repository.findByParentOrderByStart(congress)).thenReturn(List.of(workshop));

    var result = handler.handle(new CopyEventCommand(congress.getId(), oneYearLater(), null));

    var saved = ArgumentCaptor.forClass(Event.class);
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
    var copies = new ArrayList<>(saved.getAllValues());
    var copiedWorkshop = copies.get(1);
    assertThat(result.subEvents()).isEqualTo(1);
    assertThat(copiedWorkshop.getParent()).isSameAs(copies.get(0));
    assertThat(copiedWorkshop.getTitle()).isEqualTo("Taller");
    assertThat(copiedWorkshop.getRegistrationMode()).isEqualTo(RegistrationMode.OWN);
    assertThat(copiedWorkshop.getStart().toInstant()).isEqualTo(oneYearLater().toInstant());
  }

  @Test
  void aSubEventIsNotCopiedOnItsOwn() {
    var workshop =
        childOf(congress, EventId.generate(), "Taller", congress.getStart(), congress.getEnd());
    when(repository.findById(workshop.getId())).thenReturn(Optional.of(workshop));

    assertThatThrownBy(
            () -> handler.handle(new CopyEventCommand(workshop.getId(), oneYearLater(), null)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnknownEventIsNotFound() {
    var id = EventId.generate();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new CopyEventCommand(id, oneYearLater(), null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
