package org.ouracademy.srtomy.accountability.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
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

  List<Accountability> findByChildId(PartyId childId);

  List<Accountability> findByChildIdAndTypeId(PartyId childId, AccountabilityTypeId typeId);

  /** How many different people hold each of the positions. */
  @Query(
      value =
          """
          select a.type as positionId, a.child_id as personId, p.name as name,
                 n.name as organizationName, u.email as email
          from accountability a
          join party p on p.id = a.child_id
          join party n on n.id = a.parent_id
          left join user_person up on up.person_id = a.child_id
          left join app_user u on u.id = up.user_id
          where a.type in (:positionIds) and a.deleted_time is null
          order by p.name
          """,
      nativeQuery = true)
  List<PositionHolder> findHoldersOf(Collection<UUID> positionIds);

  interface PositionHolder {
    UUID getPositionId();

    UUID getPersonId();

    String getName();

    String getOrganizationName();

    /** Null when no user is linked to the person. */
    String getEmail();
  }
}
