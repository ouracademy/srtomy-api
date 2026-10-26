package org.ouracademy.ovejafiel.accountability.repository;

import java.util.List;
import java.util.Set;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityTypeRepository
    extends JpaRepository<AccountabilityType, AccountabilityTypeId> {
  List<AccountabilityType> findByParty(Party party);

  List<AccountabilityType> findByPartyIdAndCategoryOrderByName(
      PartyId partyId, AccountabilityType.Category category);

  boolean existsByPartyIdAndCategoryAndNameIgnoreCase(
      PartyId partyId, AccountabilityType.Category category, String name);

  List<AccountabilityType> findByIdInAndPartyIdAndCategory(
      Set<AccountabilityTypeId> ids, PartyId partyId, AccountabilityType.Category category);
}
