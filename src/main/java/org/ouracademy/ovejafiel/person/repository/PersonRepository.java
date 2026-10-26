package org.ouracademy.ovejafiel.person.repository;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
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

  /** The people with that email (ignoring case) who have any accountability in the node. */
  @Query(
      """
            select p
            from Person p
            where lower(p.email) = lower(:email)
            and p.id in (select a.child.id from Accountability a where a.parent.id = :organizationId)
            """)
  List<Person> findByEmailAndOrganization(String email, PartyId organizationId);
}
