package org.ouracademy.ovejafiel.accountability.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  /** Closes the open accountabilities of the child under any of the parents; returns how many. */
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update Accountability a set a.deletedTime = :endedAt
      where a.child.id = :childId and a.parent.id in :parentIds
      """)
  int endByChildIdAndParentIdIn(
      PartyId childId, Collection<PartyId> parentIds, LocalDateTime endedAt);

  List<Accountability> findByChildIdAndParentIdInAndTypeIdIn(
      PartyId childId, Collection<PartyId> parentIds, Collection<AccountabilityTypeId> typeIds);

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
