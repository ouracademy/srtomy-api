package org.ouracademy.ovejafiel.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRepository extends JpaRepository<Organization, PartyId> {

  List<Organization> findByParentIdOrderByName(PartyId parentId);

  /** The root (church) of the tree {@code id} belongs to; empty if {@code id} does not exist. */
  @Query(
      value =
          """
          with recursive ancestors as (
              select o.id, o.parent_id from organization o where o.id = :id
              union all
              select o.id, o.parent_id from organization o
              join ancestors a on o.id = a.parent_id
          )
          select a.id from ancestors a where a.parent_id is null
          """,
      nativeQuery = true)
  Optional<UUID> findChurchIdOf(UUID id);

  /** {@code id} and every node below it, however deep. */
  @Query(
      value =
          """
          with recursive subtree as (
              select o.id from organization o where o.id = :id
              union all
              select o.id from organization o
              join subtree s on o.parent_id = s.id
          )
          select s.id from subtree s
          """,
      nativeQuery = true)
  List<UUID> findSubtreeIds(UUID id);

  /** The nodes above {@code id}, the church first and the direct parent last; not {@code id}. */
  @Query(
      value =
          """
          with recursive ancestors as (
              select o.id, o.parent_id, 0 as depth from organization o where o.id = :id
              union all
              select o.id, o.parent_id, a.depth + 1 from organization o
              join ancestors a on o.id = a.parent_id
          )
          select a.id from ancestors a where a.id <> :id order by a.depth desc
          """,
      nativeQuery = true)
  List<UUID> findAncestorIds(UUID id);
}
