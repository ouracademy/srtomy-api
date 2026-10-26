package org.ouracademy.ovejafiel.iam.query.authorize;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthorizeQueryHandlerImpl implements AuthorizeQueryHandler {

  private final RoleBindingRepository roleBindingRepository;

  @Override
  @Transactional(readOnly = true)
  public AuthorizeResult handle(AuthorizeQuery query) {
    var organizationIds = query.organizationIds().stream().map(PartyId::value).toList();
    if (!organizationIds.isEmpty()
        && roleBindingRepository
            .findPermissions(query.subject(), organizationIds)
            .contains(query.permission().value())) {
      return new AuthorizeResult(query.permission());
    }
    throw new AccessDeniedException("No tienes permiso para realizar esta acción");
  }
}
