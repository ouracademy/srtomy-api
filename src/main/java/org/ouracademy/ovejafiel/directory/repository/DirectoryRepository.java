package org.ouracademy.ovejafiel.directory.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Reads across organizations, accountabilities and people to build the directory and the public
 * pages. Read-only: nothing here writes.
 */
public interface DirectoryRepository extends Repository<Organization, PartyId> {

  /** Who a person row is about, with what is needed to decide whether to list them. */
  record PersonRow(
      PartyId nodeId,
      PartyId personId,
      String firstName,
      String lastName,
      Integer age,
      LocalDate birthdate) {}

  /** A person holding a position (cargo) in a node. */
  record HeldRow(
      PartyId nodeId,
      String position,
      PartyId personId,
      String firstName,
      String lastName,
      Integer age,
      LocalDate birthdate) {}

  List<Organization> findByIdIn(Collection<PartyId> ids);

  Optional<Organization> findBySlug(String slug);

  List<Organization> findByParentIdOrderByName(PartyId parentId);

  /** The people related to the nodes by one accountability type (members or leaders). */
  @Query(
      """
      select new org.ouracademy.ovejafiel.directory.repository.DirectoryRepository$PersonRow(
          a.parent.id, p.id, p.firstName, p.lastName, p.age, p.birthdate)
      from Accountability a join Person p on p.id = a.child.id
      where a.parent.id in :nodeIds and a.type.id = :typeId and a.deletedTime is null
      order by p.firstName, p.lastName
      """)
  List<PersonRow> findPeople(Collection<PartyId> nodeIds, AccountabilityTypeId typeId);

  /** The ministerial positions (cargos) the people hold in the nodes. */
  @Query(
      """
      select new org.ouracademy.ovejafiel.directory.repository.DirectoryRepository$HeldRow(
          a.parent.id, t.name, p.id, p.firstName, p.lastName, p.age, p.birthdate)
      from Accountability a join Person p on p.id = a.child.id join a.type t
      where a.parent.id in :nodeIds
        and t.category = org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category.POSITION
        and a.deletedTime is null
      order by t.name, p.firstName, p.lastName
      """)
  List<HeldRow> findHeldPositions(Collection<PartyId> nodeIds);
}
