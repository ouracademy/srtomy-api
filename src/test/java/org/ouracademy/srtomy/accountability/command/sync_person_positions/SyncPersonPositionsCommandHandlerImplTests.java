package org.ouracademy.srtomy.accountability.command.sync_person_positions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SyncPersonPositionsCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  @Mock EntityManager entityManager;
  @InjectMocks SyncPersonPositionsCommandHandlerImpl handler;

  @Test
  @SuppressWarnings("unchecked")
  void keepsOnlyTheRequestedPositionsOfTheChurch() {
    var churchId = PartyId.generate();
    var church = party(churchId, "Iglesia");
    var person = party(PartyId.generate(), "Ana");
    var location = party(PartyId.generate(), "Sede");
    var pastor = new AccountabilityType("Pastor", church, Category.POSITION);
    var usher = new AccountabilityType("Ujier", church, Category.POSITION);
    var foreign = AccountabilityTypeId.generate();
    var heldUsher = new Accountability(person, location, usher);
    when(accountabilityRepository.findByParentIdAndChildId(location.getId(), person.getId()))
        .thenReturn(List.of(heldUsher));
    when(accountabilityTypeRepository.findByIdInAndPartyIdAndCategory(
            Set.of(pastor.getId(), foreign), churchId, Category.POSITION))
        .thenReturn(List.of(pastor));
    when(entityManager.getReference(Party.class, person.getId())).thenReturn(person);
    when(entityManager.getReference(Party.class, location.getId())).thenReturn(location);

    var result =
        handler.handle(
            new SyncPersonPositionsCommand(
                "me", person.getId(), location.getId(), churchId, Set.of(pastor.getId(), foreign)));

    assertThat(result).isEqualTo(new SyncPersonPositionsResult(1, 1));
    verify(accountabilityRepository).deleteAll(List.of(heldUsher));
    ArgumentCaptor<List<Accountability>> added = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).saveAll(added.capture());
    assertThat(added.getValue()).extracting(Accountability::getType).containsExactly(pastor);
  }

  @Test
  void anEmptySetRemovesEveryPosition() {
    when(accountabilityRepository.findByParentIdAndChildId(any(), any())).thenReturn(List.of());

    var result =
        handler.handle(
            new SyncPersonPositionsCommand(
                "me", PartyId.generate(), PartyId.generate(), PartyId.generate(), Set.of()));

    assertThat(result).isEqualTo(new SyncPersonPositionsResult(0, 0));
    verify(accountabilityRepository).saveAll(anyList());
  }

  @Test
  void addingAPositionWithAccessIsAuthorizedOnTheNode() {
    var churchId = PartyId.generate();
    var church = party(churchId, "Iglesia");
    var person = party(PartyId.generate(), "Ana");
    var location = party(PartyId.generate(), "Sede");
    var pastor = new AccountabilityType("Pastor", church, Category.POSITION, PredefinedRoles.ADMIN);
    when(accountabilityRepository.findByParentIdAndChildId(location.getId(), person.getId()))
        .thenReturn(List.of());
    when(accountabilityTypeRepository.findByIdInAndPartyIdAndCategory(
            Set.of(pastor.getId()), churchId, Category.POSITION))
        .thenReturn(List.of(pastor));
    when(entityManager.getReference(Party.class, person.getId())).thenReturn(person);
    when(entityManager.getReference(Party.class, location.getId())).thenReturn(location);

    handler.handle(
        new SyncPersonPositionsCommand(
            "me", person.getId(), location.getId(), churchId, Set.of(pastor.getId())));

    verify(authorizeAccessGrantQueryHandler)
        .handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.ADMIN, location.getId()));
  }

  @Test
  void aCallerWhoCannotGrantTheLevelChangesNothing() {
    var churchId = PartyId.generate();
    var church = party(churchId, "Iglesia");
    var person = party(PartyId.generate(), "Ana");
    var location = party(PartyId.generate(), "Sede");
    var pastor = new AccountabilityType("Pastor", church, Category.POSITION, PredefinedRoles.ADMIN);
    when(accountabilityRepository.findByParentIdAndChildId(location.getId(), person.getId()))
        .thenReturn(List.of());
    when(accountabilityTypeRepository.findByIdInAndPartyIdAndCategory(
            Set.of(pastor.getId()), churchId, Category.POSITION))
        .thenReturn(List.of(pastor));
    when(authorizeAccessGrantQueryHandler.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SyncPersonPositionsCommand(
                        "me", person.getId(), location.getId(), churchId, Set.of(pastor.getId()))))
        .isInstanceOf(AccessDeniedException.class);
    verify(accountabilityRepository, never()).saveAll(anyList());
    verify(accountabilityRepository, never()).deleteAll(anyList());
  }

  @Test
  void removingAPositionWithAccessIsAuthorizedToo() {
    var churchId = PartyId.generate();
    var church = party(churchId, "Iglesia");
    var person = party(PartyId.generate(), "Ana");
    var location = party(PartyId.generate(), "Sede");
    var pastor = new AccountabilityType("Pastor", church, Category.POSITION, PredefinedRoles.ADMIN);
    when(accountabilityRepository.findByParentIdAndChildId(location.getId(), person.getId()))
        .thenReturn(List.of(new Accountability(person, location, pastor)));
    when(entityManager.getReference(Party.class, person.getId())).thenReturn(person);
    when(entityManager.getReference(Party.class, location.getId())).thenReturn(location);

    handler.handle(
        new SyncPersonPositionsCommand("me", person.getId(), location.getId(), churchId, Set.of()));

    verify(authorizeAccessGrantQueryHandler)
        .handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.ADMIN, location.getId()));
  }
}
