package org.ouracademy.srtomy.accountability.repository;

import java.util.Collection;
import java.util.List;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountabilityRepository extends JpaRepository<Accountability, AccountabilityId> {

  List<Accountability> findByParentIdAndTypeIn(PartyId id, List<AccountabilityType> types);

  List<Accountability> findByParentIdAndTypeIdIn(
      PartyId parentId, Collection<AccountabilityTypeId> typeIds);

  void deleteAllByType(AccountabilityType roleToDelete);

  List<Accountability> findByParentIdAndChildId(PartyId parentId, PartyId childId);

  void deleteAllByParentId(PartyId parentId);

  void deleteAllByChildId(PartyId childId);

  List<Accountability> findByChildIdAndTypeId(PartyId childId, AccountabilityTypeId typeId);

  /** How many different people hold each of the positions. */
  @Query(
      """
      select a.type.id as positionId, count(distinct a.child.id) as holders
      from Accountability a
      where a.type.id in :positionIds
      group by a.type.id
      """)
  List<PositionHolders> countHoldersOf(Collection<AccountabilityTypeId> positionIds);

  interface PositionHolders {
    AccountabilityTypeId getPositionId();

    long getHolders();
  }
}
