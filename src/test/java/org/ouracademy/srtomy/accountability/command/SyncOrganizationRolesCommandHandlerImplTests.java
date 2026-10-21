package org.ouracademy.srtomy.accountability.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class SyncOrganizationRolesCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock EntityManager entityManager;

  SyncOrganizationRolesCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new SyncOrganizationRolesCommandHandlerImpl(
            accountabilityTypeRepository, accountabilityRepository, entityManager);
  }

  @Test
  void handleDeletesRolesNoLongerListedAndCreatesTheNewOnesOwnedByTheLocation() {
    var locationId = PartyId.generate();
    var location = party(locationId, "Location");
    var removedRole =
        withId(
            new AccountabilityType("GREETER", location, Category.ROLE),
            AccountabilityTypeId.generate());
    var keptRole =
        withId(
            new AccountabilityType("SOUND", location, Category.ROLE),
            AccountabilityTypeId.generate());

    when(entityManager.getReference(Party.class, locationId)).thenReturn(location);
    when(accountabilityTypeRepository.findByParty(location))
        .thenReturn(List.of(removedRole, keptRole));

    var result =
        handler.handle(
            new SyncOrganizationRolesCommand(locationId, new TreeSet<>(List.of("SOUND", "CHOIR"))));

    assertThat(result).isEqualTo(new SyncOrganizationRolesResult(1, 1));
    verify(accountabilityTypeRepository).delete(removedRole);
    verify(accountabilityRepository).deleteAllByType(removedRole);
    verify(accountabilityTypeRepository, never()).delete(keptRole);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AccountabilityType>> savedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityTypeRepository).saveAll(savedCaptor.capture());
    assertThat(savedCaptor.getValue()).hasSize(1);
    var created = savedCaptor.getValue().get(0);
    assertThat(created.getName()).isEqualTo("CHOIR");
    assertThat(created.getParty()).isSameAs(location);
    assertThat(created.getCategory()).isEqualTo(Category.ROLE);
  }
}
