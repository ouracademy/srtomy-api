package org.ouracademy.srtomy.accountability.repository;

import java.util.Collection;
import java.util.List;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityRepository extends JpaRepository<Accountability, AccountabilityId> {

  List<Accountability> findByParentIdAndTypeIn(PartyId id, List<AccountabilityType> types);

  List<Accountability> findByParentIdAndTypeIdIn(
      PartyId parentId, Collection<AccountabilityTypeId> typeIds);

  void deleteAllByType(AccountabilityType roleToDelete);

  List<Accountability> findByParentIdAndChildId(PartyId parentId, PartyId childId);

  void deleteAllByParentId(PartyId parentId);

  void deleteAllByChildId(PartyId childId);

  List<Accountability> findByChildIdAndTypeId(PartyId childId, AccountabilityTypeId typeId);
}
