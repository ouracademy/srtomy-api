package org.ouracademy.ovejafiel.accountability.command.create_accountability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.AccountabilityFixtures.party;
import static org.ouracademy.ovejafiel.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateAccountabilityCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;
  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock EntityManager entityManager;

  CreateAccountabilityCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateAccountabilityCommandHandlerImpl(
            accountabilityRepository, accountabilityTypeRepository, entityManager);
  }

  @Test
  void handleSavesAnAccountabilityBetweenTheReferencedPartiesAndReturnsItsId() {
    var childId = PartyId.generate();
    var parentId = PartyId.generate();
    var child = party(childId, "Child");
    var parent = party(parentId, "Parent");
    var type = withId(new AccountabilityType("IS_PART_OF", null, null), IS_PART_OF);
    when(entityManager.getReference(Party.class, childId)).thenReturn(child);
    when(entityManager.getReference(Party.class, parentId)).thenReturn(parent);
    when(accountabilityTypeRepository.getReferenceById(IS_PART_OF)).thenReturn(type);
    when(accountabilityRepository.save(any(Accountability.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = handler.handle(new CreateAccountabilityCommand(childId, parentId, IS_PART_OF));

    var captor = ArgumentCaptor.forClass(Accountability.class);
    verify(accountabilityRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getChild()).isSameAs(child);
    assertThat(saved.getParent()).isSameAs(parent);
    assertThat(saved.getType()).isSameAs(type);
    assertThat(result).isEqualTo(new CreateAccountabilityResult(saved.getId()));
  }
}
