package org.ouracademy.srtomy.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityTypeRepository extends JpaRepository<AccountabilityType, Long> {
  List<AccountabilityType> findByParty(Party party);
}
