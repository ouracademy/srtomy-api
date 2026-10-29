package org.ouracademy.ovejafiel.person.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, PartyId> {

  /**
   * People that belong to any of {@code organizationIds}, each once even if they belong to several,
   * ordered by name.
   */
  @Query(
      """
            select p
            from Person p
            where p.id in (
                select a.child.id from Accountability a
                where a.type = :accountabilityType
                and a.parent.id in :organizationIds
            )
            and (
                lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
                or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            )
            order by p.firstName, p.lastName, p.id
            """)
  Page<Person> findByNameAndOrgIds(
      String searchText,
      List<PartyId> organizationIds,
      AccountabilityType accountabilityType,
      Pageable pageable);

  @Query(
      """
            select p
            from Person p
            where p.id in (
                select a.child.id from Accountability a
                where a.type = :accountabilityType
                and a.parent.id in :organizationIds
            )
            order by p.firstName, p.lastName, p.id
            """)
  Page<Person> findByOrgIds(
      List<PartyId> organizationIds, AccountabilityType accountabilityType, Pageable pageable);

  /**
   * Like {@link #findByOrgIds} but only those whose state is {@code rank} (1 visitor, 2 attendee, 3
   * member): the highest state they have in any of {@code organizationIds}.
   */
  @Query(
      """
            select p
            from Person p
            where p.id in (
                select a.child.id from Accountability a
                where a.type = :accountabilityType
                and a.parent.id in :organizationIds
            )
            and (
                select max(case when s.type.id = :memberType then 3
                                when s.type.id = :attendeeType then 2 else 1 end)
                from Accountability s
                where s.child.id = p.id and s.parent.id in :organizationIds
                and s.type.id in :stateTypes
            ) = :rank
            order by p.firstName, p.lastName, p.id
            """)
  Page<Person> findByOrgIdsInState(
      List<PartyId> organizationIds,
      AccountabilityType accountabilityType,
      AccountabilityTypeId memberType,
      AccountabilityTypeId attendeeType,
      List<AccountabilityTypeId> stateTypes,
      int rank,
      Pageable pageable);

  /** Like {@link #findByNameAndOrgIds} but only those whose state is {@code rank}. */
  @Query(
      """
            select p
            from Person p
            where p.id in (
                select a.child.id from Accountability a
                where a.type = :accountabilityType
                and a.parent.id in :organizationIds
            )
            and (
                select max(case when s.type.id = :memberType then 3
                                when s.type.id = :attendeeType then 2 else 1 end)
                from Accountability s
                where s.child.id = p.id and s.parent.id in :organizationIds
                and s.type.id in :stateTypes
            ) = :rank
            and (
                lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
                or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            )
            order by p.firstName, p.lastName, p.id
            """)
  Page<Person> findByNameAndOrgIdsInState(
      String searchText,
      List<PartyId> organizationIds,
      AccountabilityType accountabilityType,
      AccountabilityTypeId memberType,
      AccountabilityTypeId attendeeType,
      List<AccountabilityTypeId> stateTypes,
      int rank,
      Pageable pageable);

  /**
   * What each of {@code personIds} has in {@code organizationIds}: their open accountabilities
   * there that are of one of {@code typeIds} (the progress states and LEADER), and in which node.
   */
  @Query(
      """
            select a.child.id as personId, a.parent.id as nodeId, a.type.id as typeId
            from Accountability a
            where a.parent.id in :organizationIds and a.child.id in :personIds
            and a.type.id in :typeIds
            """)
  List<Standing> findStandings(
      List<PartyId> personIds, List<PartyId> organizationIds, List<AccountabilityTypeId> typeIds);

  interface Standing {
    PartyId getPersonId();

    PartyId getNodeId();

    AccountabilityTypeId getTypeId();
  }

  /** The people with that email (ignoring case) who have any accountability in the node. */
  @Query(
      """
            select p
            from Person p
            where lower(p.email) = lower(:email)
            and p.id in (select a.child.id from Accountability a where a.parent.id = :organizationId)
            """)
  List<Person> findByEmailAndOrganization(String email, PartyId organizationId);

  /**
   * People of those organizations (open or closed membership), by name containing {@code name} or
   * by email, for telling someone that the person they are registering may already exist. Native on
   * purpose: closed memberships are invisible to JPQL. A blank {@code name} or {@code email} (never
   * null) does not filter.
   */
  @Query(
      value =
          """
          select p.id as id, pa.name as fullName, p.email as email,
                 pa.phone_number as phoneNumber,
                 exists (select 1 from accountability a
                         where a.child_id = p.id and a.deleted_time is null
                           and a.parent_id in (:organizationIds)) as active,
                 coalesce(p.deleted_time,
                          (select max(a.deleted_time) from accountability a
                           where a.child_id = p.id and a.parent_id in (:organizationIds))) as removedTime
          from person p
          join party pa on pa.id = p.id
          where exists (select 1 from accountability a
                        where a.child_id = p.id and a.parent_id in (:organizationIds))
            and ((:name <> '' and lower(pa.name) like lower(concat('%', :name, '%')))
                 or (:email <> '' and lower(p.email) = lower(:email)))
          order by active desc, pa.name, p.id
          limit :max
          """,
      nativeQuery = true)
  List<PersonMatch> findMatches(String name, String email, List<UUID> organizationIds, int max);

  interface PersonMatch {
    UUID getId();

    String getFullName();

    String getEmail();

    String getPhoneNumber();

    Boolean getActive();

    LocalDateTime getRemovedTime();
  }
}
