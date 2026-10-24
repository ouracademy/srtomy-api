package org.ouracademy.srtomy.accountability.repository;

import java.util.List;
import java.util.Set;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
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
