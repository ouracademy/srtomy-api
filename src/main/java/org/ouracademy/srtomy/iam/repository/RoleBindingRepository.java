package org.ouracademy.srtomy.iam.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.RoleBinding;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Bindings are inherited down the tree, so every read walks {@code organization.parent_id} with a
 * recursive CTE (read-only native SQL over the organization table, allowed by the module rules).
 */
public interface RoleBindingRepository extends JpaRepository<RoleBinding, RoleBindingId> {

  boolean existsByUserIdAndRoleIdAndOrganizationId(
      UserId userId, IamRoleId roleId, PartyId organizationId);

  /**
   * Permission values the user identified by {@code subject} holds on any of {@code
   * organizationIds}, through a binding on that node or on one of its ancestors.
   */
  @Query(
      value =
          """
          with recursive ancestors as (
              select o.id, o.parent_id from organization o where o.id in (:organizationIds)
              union
              select o.id, o.parent_id from organization o
              join ancestors a on o.id = a.parent_id
          )
          select distinct p.permission
          from role_binding rb
          join app_user u on u.id = rb.user_id
          join iam_role r on r.id = rb.role_id and r.deleted_time is null
          cross join lateral unnest(r.permissions) as p(permission)
          where u.auth0_sub = :subject
          and rb.organization_id in (select id from ancestors)
          """,
      nativeQuery = true)
  List<String> findPermissions(String subject, Collection<UUID> organizationIds);

  /**
   * Every node the user can see: those with a binding and their descendants ({@code accessible}),
   * plus their ancestors, which are only shown as a breadcrumb.
   */
  @Query(
      value =
          """
          with recursive bound as (
              select rb.organization_id as id
              from role_binding rb
              join app_user u on u.id = rb.user_id
              join iam_role r on r.id = rb.role_id and r.deleted_time is null
              where u.auth0_sub = :subject
          ),
          descendants as (
              select o.id from organization o where o.id in (select id from bound)
              union
              select o.id from organization o join descendants d on o.parent_id = d.id
          ),
          ancestors as (
              select o.id, o.parent_id from organization o where o.id in (select id from bound)
              union
              select o.id, o.parent_id from organization o
              join ancestors a on o.id = a.parent_id
          )
          select o.id as id, p.name as name, o.type as type, o.parent_id as parentId,
                 (o.id in (select id from descendants)) as accessible
          from organization o
          join party p on p.id = o.id
          where o.id in (select id from descendants) or o.id in (select id from ancestors)
          order by p.name
          """,
      nativeQuery = true)
  List<VisibleOrganization> findVisibleOrganizations(String subject);

  /** The roles bound directly on each node, for the user identified by {@code subject}. */
  @Query(
      """
      select rb.organization.id as organizationId, rb.role.name as roleName
      from RoleBinding rb
      where rb.user.subject = :subject
      order by rb.role.name
      """)
  List<BoundRole> findBoundRoles(String subject);

  /**
   * The bindings on the node and on all its ancestors (live roles only), with who holds them and
   * where each one was made.
   */
  @Query(
      value =
          """
          with recursive ancestors as (
              select o.id, o.parent_id from organization o where o.id = :organizationId
              union
              select o.id, o.parent_id from organization o
              join ancestors a on o.id = a.parent_id
          )
          select rb.id as bindingId, u.id as userId, u.email as email,
                 r.id as roleId, r.name as roleName,
                 rb.organization_id as organizationId, p.name as organizationName
          from role_binding rb
          join app_user u on u.id = rb.user_id
          join iam_role r on r.id = rb.role_id and r.deleted_time is null
          join party p on p.id = rb.organization_id
          where rb.organization_id in (select id from ancestors)
          order by lower(u.email), r.name
          """,
      nativeQuery = true)
  List<AccessEntry> findAccess(UUID organizationId);

  /** Whether the user holds {@code roleId} on the node or on one of its ancestors. */
  @Query(
      value =
          """
          with recursive ancestors as (
              select o.id, o.parent_id from organization o where o.id = :organizationId
              union
              select o.id, o.parent_id from organization o
              join ancestors a on o.id = a.parent_id
          )
          select count(*) > 0
          from role_binding rb
          join app_user u on u.id = rb.user_id
          where u.auth0_sub = :subject and rb.role_id = :roleId
          and rb.organization_id in (select id from ancestors)
          """,
      nativeQuery = true)
  boolean holdsRole(String subject, UUID roleId, UUID organizationId);

  /** How many bindings of {@code roleId} exist on the church and every node below it. */
  @Query(
      value =
          """
          with recursive tree as (
              select o.id from organization o where o.id = :churchId
              union
              select o.id from organization o join tree t on o.parent_id = t.id
          )
          select count(*) from role_binding rb
          where rb.role_id = :roleId and rb.organization_id in (select id from tree)
          """,
      nativeQuery = true)
  long countByRoleInChurch(UUID roleId, UUID churchId);

  interface AccessEntry {
    UUID getBindingId();

    UUID getUserId();

    String getEmail();

    UUID getRoleId();

    String getRoleName();

    UUID getOrganizationId();

    String getOrganizationName();
  }

  interface VisibleOrganization {
    UUID getId();

    String getName();

    String getType();

    UUID getParentId();

    boolean getAccessible();
  }

  interface BoundRole {
    PartyId getOrganizationId();

    String getRoleName();
  }
}
