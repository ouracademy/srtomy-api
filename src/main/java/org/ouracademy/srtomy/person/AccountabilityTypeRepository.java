package org.ouracademy.srtomy.person;

import java.util.List;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityTypeRepository
    extends JpaRepository<AccountabilityType, AccountabilityTypeId> {
  List<AccountabilityType> findByParty(Party party);
}
