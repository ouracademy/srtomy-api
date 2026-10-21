package org.ouracademy.srtomy.person;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityRepository extends JpaRepository<Accountability, AccountabilityId> {

  List<Accountability> findByParentIdAndTypeIn(PartyId id, List<AccountabilityType> types);

  void deleteAllByType(AccountabilityType roleToDelete);

  List<Accountability> findByParentIdAndChildId(PartyId parentId, PartyId childId);

  void deleteAllByParentId(PartyId parentId);

  void deleteAllByChildId(PartyId childId);
}
