package org.ouracademy.ovejafiel.accountability.command.set_position_access;

import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetPositionAccessCommandHandlerImpl implements SetPositionAccessCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;

  @Override
  @Transactional
  public Optional<SetPositionAccessResult> handle(SetPositionAccessCommand command) {
    return accountabilityTypeRepository
        .findById(command.positionId())
        .filter(AccountabilityType::isPosition)
        .map(
            position -> {
              var churchId = position.getParty().getId();
              requireCanGrant(
                  command.subject(),
                  Set.copyOf(position.getRoleIds()),
                  Set.copyOf(position.getPermissions()),
                  churchId);
              requireCanGrant(
                  command.subject(), command.roleIds(), command.permissions(), churchId);
              position.setAccess(command.roleIds(), command.permissions());
              return new SetPositionAccessResult(PositionResult.from(position));
            });
  }

  private void requireCanGrant(
      String subject, Set<IamRoleId> roleIds, Set<String> permissions, PartyId churchId) {
    if (!roleIds.isEmpty() || !permissions.isEmpty()) {
      authorizeAccessGrantQueryHandler.handle(
          new AuthorizeAccessGrantQuery(subject, roleIds, permissions, churchId));
    }
  }
}
