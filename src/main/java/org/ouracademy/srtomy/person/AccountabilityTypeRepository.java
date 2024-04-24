package org.ouracademy.srtomy.person;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountabilityTypeRepository extends JpaRepository<AccountabilityType, Long> {
    List<AccountabilityType> findByParty(Party party);
}
