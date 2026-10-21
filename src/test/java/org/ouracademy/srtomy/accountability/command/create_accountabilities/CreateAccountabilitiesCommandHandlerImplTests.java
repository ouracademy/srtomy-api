package org.ouracademy.srtomy.accountability.command.create_accountabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.withId;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateAccountabilitiesCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;
  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock EntityManager entityManager;

  CreateAccountabilitiesCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateAccountabilitiesCommandHandlerImpl(
            accountabilityRepository, accountabilityTypeRepository, entityManager);
  }

  @Test
  void handleSavesAllItemsInASingleBatchAndReturnsTheirIdsInOrder() {
    var memberId = PartyId.generate();
    var leaderId = PartyId.generate();
    var groupId = PartyId.generate();
    var member = party(memberId, "Member");
    var leader = party(leaderId, "Leader");
    var group = party(groupId, "Group");
    var isPartOf = withId(new AccountabilityType("IS_PART_OF", null, null), IS_PART_OF);
    var leads = withId(new AccountabilityType("LEADER", null, null), LEADER);
    when(entityManager.getReference(Party.class, memberId)).thenReturn(member);
    when(entityManager.getReference(Party.class, leaderId)).thenReturn(leader);
    when(entityManager.getReference(Party.class, groupId)).thenReturn(group);
    when(accountabilityTypeRepository.getReferenceById(IS_PART_OF)).thenReturn(isPartOf);
    when(accountabilityTypeRepository.getReferenceById(LEADER)).thenReturn(leads);
    when(accountabilityRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        handler.handle(
            new CreateAccountabilitiesCommand(
                List.of(
                    new CreateAccountabilityCommand(memberId, groupId, IS_PART_OF),
                    new CreateAccountabilityCommand(leaderId, groupId, LEADER))));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> captor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).saveAll(captor.capture());
    var saved = captor.getValue();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).getChild()).isSameAs(member);
    assertThat(saved.get(0).getType()).isSameAs(isPartOf);
    assertThat(saved.get(1).getChild()).isSameAs(leader);
    assertThat(saved.get(1).getType()).isSameAs(leads);
    assertThat(saved).allSatisfy(a -> assertThat(a.getParent()).isSameAs(group));
    assertThat(result.ids()).containsExactly(saved.get(0).getId(), saved.get(1).getId());
  }
}
