package org.ouracademy.srtomy.person.repository;

import java.util.List;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PersonRepository extends JpaRepository<Person, PartyId> {

  /**
   * A person listed under a node of a subtree. {@code organizationName} is one of the nodes the
   * person belongs to; {@code directMembership} is 1 when they belong to {@code rootId} itself.
   * Only the id is selected: grouping by a joined-inheritance entity is not valid SQL, so callers
   * load the people with {@code findAllById}.
   */
  record PersonInSubtree(PartyId personId, String organizationName, int directMembership) {}

  @Query(
      value =
          """
            select new org.ouracademy.srtomy.person.repository.PersonRepository$PersonInSubtree(
                p.id, min(a.parent.name),
                max(case when a.parent.id = :rootId then 1 else 0 end))
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id in :organizationIds
            and (
                lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
                or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            )
            group by p.id
            order by min(p.firstName), min(p.lastName), p.id
            """,
      countQuery =
          """
            select count(distinct p)
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id in :organizationIds
            and (
                lower(p.firstName) like lower(CONCAT('%', :searchText, '%'))
                or lower(p.lastName) like lower(CONCAT('%', :searchText, '%'))
            )
            """)
  Page<PersonInSubtree> findByNameAndOrgIds(
      String searchText,
      PartyId rootId,
      List<PartyId> organizationIds,
      AccountabilityType accountabilityType,
      Pageable pageable);

  @Query(
      value =
          """
            select new org.ouracademy.srtomy.person.repository.PersonRepository$PersonInSubtree(
                p.id, min(a.parent.name),
                max(case when a.parent.id = :rootId then 1 else 0 end))
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id in :organizationIds
            group by p.id
            order by min(p.firstName), min(p.lastName), p.id
            """,
      countQuery =
          """
            select count(distinct p)
            from Accountability a join TREAT(a.child as Person) p
            where a.type = :accountabilityType
            and a.parent.id in :organizationIds
            """)
  Page<PersonInSubtree> findByOrgIds(
      PartyId rootId,
      List<PartyId> organizationIds,
      AccountabilityType accountabilityType,
      Pageable pageable);
}
