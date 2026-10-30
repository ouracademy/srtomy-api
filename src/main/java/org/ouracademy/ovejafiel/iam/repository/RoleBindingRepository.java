package org.ouracademy.ovejafiel.iam.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.RoleBinding;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Access is inherited down the tree, so every read walks {@code organization.parent_id} with a
 * recursive CTE (read-only native SQL over the organization table, allowed by the module rules).
 *
 * <p>What a user may do comes from the {@code user_access} view, which merges direct bindings
 * (Owner), the access level of the positions of the person that represents the user, and group
 * leadership, and the Member level every linked person has on the nodes they belong to. {@code
 * role_binding} itself only holds the direct ones.
 */
public interface RoleBindingRepository extends JpaRepository<RoleBinding, RoleBindingId> {

  boolean existsByUserIdAndRoleIdAndOrganizationId(
      UserId userId, IamRoleId roleId, PartyId organizationId);

  /**
   * Permission values the user identified by {@code subject} holds on any of {@code
   * organizationIds}, through an access on that node or on one of its ancestors.
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
          from user_access ua
          join app_user u on u.id = ua.user_id
          cross join lateral unnest(ua.permissions) as p(permission)
          where u.auth0_sub = :subject
          and ua.organization_id in (select id from ancestors)
          """,
      nativeQuery = true)
  List<String> findPermissions(String subject, Collection<UUID> organizationIds);

  /**
   * Every node the user can see: those with an access and their descendants ({@code accessible}),
   * plus their ancestors, which are only shown as a breadcrumb.
   */
  @Query(
      value =
          """
          with recursive bound as (
              select ua.organization_id as id
              from user_access ua
              join app_user u on u.id = ua.user_id
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

  /** What gives the user access on each node: a role name, or the name of the position. */
  @Query(
      value =
          """
          select distinct ua.organization_id as organizationId,
                 coalesce(r.name, ua.position_name) as roleName
          from user_access ua
          join app_user u on u.id = ua.user_id
          left join iam_role r on r.id = ua.role_id
          where u.auth0_sub = :subject
          order by roleName
          """,
      nativeQuery = true)
  List<BoundRole> findBoundRoles(String subject);

  /**
   * The accesses on the node and on all its ancestors, with who holds them, where each one was
   * made, where it comes from and how many permissions it gives. The implicit Member level of
   * whoever belongs to a node is left out: it is not a grant anyone made.
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
          select ua.binding_id as bindingId, u.id as userId, u.email as email,
                 ua.role_id as roleId, r.name as roleName,
                 ua.organization_id as organizationId, p.name as organizationName,
                 ua.source as source, ua.position_name as positionName,
                 cardinality(ua.permissions) as permissionCount
          from user_access ua
          join app_user u on u.id = ua.user_id
          left join iam_role r on r.id = ua.role_id
          join party p on p.id = ua.organization_id
          where ua.organization_id in (select id from ancestors)
          and ua.source <> 'MEMBER'
          order by lower(u.email), r.name, ua.position_name
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
          from user_access ua
          join app_user u on u.id = ua.user_id
          where u.auth0_sub = :subject and ua.role_id = :roleId
          and ua.organization_id in (select id from ancestors)
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
    /** Only set for direct bindings, which are the only ones that can be revoked one by one. */
    UUID getBindingId();

    UUID getUserId();

    String getEmail();

    /** Null for an access that comes from a position, which can mix several roles. */
    UUID getRoleId();

    /** Null for an access that comes from a position. */
    String getRoleName();

    UUID getOrganizationId();

    String getOrganizationName();

    /** {@code DIRECT} or {@code POSITION}. */
    String getSource();

    /** The position the access comes from; only set for {@code POSITION}. */
    String getPositionName();

    int getPermissionCount();
  }

  interface VisibleOrganization {
    UUID getId();

    String getName();

    String getType();

    UUID getParentId();

    boolean getAccessible();
  }

  interface BoundRole {
    UUID getOrganizationId();

    String getRoleName();
  }
}
