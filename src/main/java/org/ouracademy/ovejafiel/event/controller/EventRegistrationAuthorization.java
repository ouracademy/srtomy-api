package org.ouracademy.ovejafiel.event.controller;

import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** The permission checks of the registration endpoints, which act on the node of an event. */
@Component
@RequiredArgsConstructor
class EventRegistrationAuthorization {

  private final FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  private final AuthorizeQueryHandler authorizeQueryHandler;

  /** 404 when the event does not exist, 403 when the user lacks the permission on its node. */
  void require(Jwt jwt, UUID eventId, Permission permission) {
    var organizationId =
        findEventOrganizationQueryHandler
            .handle(new FindEventOrganizationQuery(new EventId(eventId)))
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId))
            .organizationId();
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }
}
