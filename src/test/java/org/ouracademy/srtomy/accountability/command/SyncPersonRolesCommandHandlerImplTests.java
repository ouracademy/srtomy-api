package org.ouracademy.srtomy.accountability.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class SyncPersonRolesCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock EntityManager entityManager;

  SyncPersonRolesCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new SyncPersonRolesCommandHandlerImpl(
            accountabilityTypeRepository, accountabilityRepository, entityManager);
  }

  @Test
  void handleRemovesRolesNotInTheNewSetAndAddsTheOnesMissing() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = party(locationId, "Location");
    var person = party(personId, "Person");

    var oldRoleTypeId = AccountabilityTypeId.generate();
    var keptRoleTypeId = AccountabilityTypeId.generate();
    var newRoleTypeId = AccountabilityTypeId.generate();
    var oldRoleType =
        withId(new AccountabilityType("USHER", location, Category.ROLE), oldRoleTypeId);
    var keptRoleType =
        withId(new AccountabilityType("PASTOR", location, Category.ROLE), keptRoleTypeId);
    var newRoleType =
        withId(new AccountabilityType("WORSHIP_LEADER", location, Category.ROLE), newRoleTypeId);

    var oldAccountability =
        withId(new Accountability(person, location, oldRoleType), AccountabilityId.generate());
    var keptAccountability =
        withId(new Accountability(person, location, keptRoleType), AccountabilityId.generate());

    when(accountabilityRepository.findByParentIdAndChildId(locationId, personId))
        .thenReturn(List.of(oldAccountability, keptAccountability));
    when(entityManager.getReference(Party.class, personId)).thenReturn(person);
    when(entityManager.getReference(Party.class, locationId)).thenReturn(location);
    when(accountabilityTypeRepository.getReferenceById(newRoleTypeId)).thenReturn(newRoleType);

    var result =
        handler.handle(
            new SyncPersonRolesCommand(
                personId, locationId, Set.of(keptRoleTypeId, newRoleTypeId)));

    assertThat(result).isEqualTo(new SyncPersonRolesResult(1, 1));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> deletedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).deleteAll(deletedCaptor.capture());
    assertThat(deletedCaptor.getValue()).containsExactly(oldAccountability);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> savedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).saveAll(savedCaptor.capture());
    assertThat(savedCaptor.getValue()).hasSize(1);
    var added = savedCaptor.getValue().get(0);
    assertThat(added.getType()).isEqualTo(newRoleType);
    assertThat(added.getChild()).isSameAs(person);
    assertThat(added.getParent()).isSameAs(location);
  }

  @Test
  void handleIgnoresAccountabilitiesThatAreNotRoles() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = party(locationId, "Location");
    var person = party(personId, "Person");
    var memberProgressType =
        withId(
            new AccountabilityType("BAPTIZED", location, Category.MEMBER_PROGRESS_STATE),
            AccountabilityTypeId.generate());
    var nonRoleAccountability =
        withId(
            new Accountability(person, location, memberProgressType), AccountabilityId.generate());

    when(accountabilityRepository.findByParentIdAndChildId(locationId, personId))
        .thenReturn(List.of(nonRoleAccountability));

    var result = handler.handle(new SyncPersonRolesCommand(personId, locationId, Set.of()));

    assertThat(result).isEqualTo(new SyncPersonRolesResult(0, 0));
    // The non-role accountability must not be touched by the role sync.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> deletedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).deleteAll(deletedCaptor.capture());
    assertThat(deletedCaptor.getValue()).isEmpty();
  }
}
