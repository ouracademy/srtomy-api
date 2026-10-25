package org.ouracademy.srtomy.accountability.query.get_person_accountabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.AccountabilityFixtures;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

class GetPersonAccountabilitiesQueryHandlerImplTests {

  AccountabilityRepository repository = mock(AccountabilityRepository.class);
  GetPersonAccountabilitiesQueryHandlerImpl handler =
      new GetPersonAccountabilitiesQueryHandlerImpl(repository);

  PartyId person = PartyId.generate();
  PartyId node = PartyId.generate();
  PartyId church = PartyId.generate();

  AccountabilityType type(AccountabilityTypeId id, boolean position) {
    var type = mock(AccountabilityType.class);
    when(type.getId()).thenReturn(id);
    when(type.isPosition()).thenReturn(position);
    when(type.getName()).thenReturn("Tipo");
    when(type.getParty()).thenReturn(AccountabilityFixtures.party(church, "Iglesia"));
    return type;
  }

  Accountability accountability(PartyId parent, AccountabilityType type) {
    return new Accountability(
        AccountabilityFixtures.party(person, "Ana"),
        AccountabilityFixtures.party(parent, "Nodo"),
        type);
  }

  @Test
  void listsEveryAccountabilityAndMarksThePositions() {
    var memberId = AccountabilityTypeId.generate();
    var positionId = AccountabilityTypeId.generate();
    var member = type(memberId, false);
    var position = type(positionId, true);
    var memberOf = accountability(node, member);
    var holds = accountability(church, position);
    when(repository.findByChildId(person)).thenReturn(List.of(memberOf, holds));

    var result = handler.handle(new GetPersonAccountabilitiesQuery(person));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).organizationId()).isEqualTo(node);
    assertThat(result.get(0).typeId()).isEqualTo(memberId);
    assertThat(result.get(0).position()).isNull();
    assertThat(result.get(1).organizationId()).isEqualTo(church);
    assertThat(result.get(1).position().id()).isEqualTo(positionId);
  }

  @Test
  void aPersonWithoutAccountabilitiesHasNone() {
    when(repository.findByChildId(person)).thenReturn(List.of());

    assertThat(handler.handle(new GetPersonAccountabilitiesQuery(person))).isEmpty();
  }
}
