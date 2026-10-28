package org.ouracademy.ovejafiel.event.query;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.stereotype.Component;

/**
 * What someone can do with an event: the most they get from the church tree ({@code events.write}
 * manages, {@code events.read} views), from the event being shared with their verified email, or
 * from its general access when it is open to the church.
 */
@Component
@RequiredArgsConstructor
public class EventAccess {

  private final GetEffectivePermissionsQueryHandler getEffectivePermissionsQueryHandler;
  private final FindAccessibleOrganizationsQueryHandler findAccessibleOrganizationsQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final EventShareRepository eventShareRepository;

  /** {@code subject} may be null for someone who is not signed in: they get nothing from here. */
  public AccessLevel levelOn(String subject, Event event) {
    if (subject == null) {
      return AccessLevel.NONE;
    }
    var level = fromTree(subject, event);
    level = level.or(fromShare(subject, event));
    if (event.getGeneralAccess() == GeneralAccess.CHURCH && isChurchMember(subject, event)) {
      level = level.or(AccessLevel.VIEWER);
    }
    return level;
  }

  /**
   * Whether {@code subject} (null: not signed in) can open the shared page of the event: the link
   * is open to anyone, or the subject has at least viewer access by the tree, a share or the
   * church.
   */
  public boolean canOpen(String subject, Event event) {
    return event.getGeneralAccess() == GeneralAccess.ANYONE_WITH_LINK
        || levelOn(subject, event).atLeast(AccessLevel.VIEWER);
  }

  /** The signed-in user, if they have verified an email. */
  public UserResult verifiedUser(String subject) {
    return subject == null
        ? null
        : findUserBySubjectQueryHandler
            .handle(new FindUserBySubjectQuery(subject))
            .filter(user -> user.emailVerified() && user.email() != null)
            .orElse(null);
  }

  private AccessLevel fromTree(String subject, Event event) {
    var permissions =
        getEffectivePermissionsQueryHandler
            .handle(new GetEffectivePermissionsQuery(subject, event.getOrganization().getId()))
            .permissions();
    if (permissions.contains(Permission.EVENTS_WRITE)) {
      return AccessLevel.MANAGER;
    }
    return permissions.contains(Permission.EVENTS_READ) ? AccessLevel.VIEWER : AccessLevel.NONE;
  }

  private AccessLevel fromShare(String subject, Event event) {
    var user = verifiedUser(subject);
    if (user == null) {
      return AccessLevel.NONE;
    }
    return eventShareRepository
        .findByEventIdAndEmailIgnoreCase(event.getId(), user.email())
        .map(
            share ->
                switch (share.getRole()) {
                  case EDITOR -> AccessLevel.EDITOR;
                  case VIEWER -> AccessLevel.VIEWER;
                })
        .orElse(AccessLevel.NONE);
  }

  private boolean isChurchMember(String subject, Event event) {
    PartyId churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(event.getOrganization().getId()))
            .map(result -> result.churchId())
            .orElse(null);
    return churchId != null
        && findAccessibleOrganizationsQueryHandler
            .handle(new FindAccessibleOrganizationsQuery(subject))
            .stream()
            .anyMatch(node -> node.id().equals(churchId));
  }
}
