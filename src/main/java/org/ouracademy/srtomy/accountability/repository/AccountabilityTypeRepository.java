package org.ouracademy.srtomy.accountability.repository;

import java.util.List;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityTypeRepository
    extends JpaRepository<AccountabilityType, AccountabilityTypeId> {
  List<AccountabilityType> findByParty(Party party);
}
