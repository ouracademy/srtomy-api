package org.ouracademy.srtomy.accountability.command.delete_position;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeletePositionCommandHandlerImpl implements DeletePositionCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;
  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;

  @Override
  @Transactional
  public DeletePositionResult handle(DeletePositionCommand command) {
    var position =
        accountabilityTypeRepository
            .findById(command.positionId())
            .filter(AccountabilityType::isPosition);
    position
        .filter(AccountabilityType::hasAccess)
        .ifPresent(
            type ->
                authorizeAccessGrantQueryHandler.handle(
                    new AuthorizeAccessGrantQuery(
                        command.subject(), type.getAccessRoleId(), type.getParty().getId())));
    position.ifPresent(
        type -> {
          accountabilityRepository.deleteAllByType(type);
          accountabilityTypeRepository.delete(type);
        });
    return new DeletePositionResult(position.isPresent());
  }
}
