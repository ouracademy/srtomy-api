package org.ouracademy.ovejafiel.accountability.command.create_position;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.PositionNames;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreatePositionCommandHandlerImpl implements CreatePositionCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final PositionNames positionNames;
  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public CreatePositionResult handle(CreatePositionCommand command) {
    if (!command.roleIds().isEmpty() || !command.permissions().isEmpty()) {
      authorizeAccessGrantQueryHandler.handle(
          new AuthorizeAccessGrantQuery(
              command.subject(), command.roleIds(), command.permissions(), command.churchId()));
    }
    var name = positionNames.requireAvailable(command.churchId(), command.name());
    var position =
        accountabilityTypeRepository.save(
            new AccountabilityType(
                name,
                entityManager.getReference(Party.class, command.churchId()),
                Category.POSITION,
                command.roleIds(),
                command.permissions()));
    return new CreatePositionResult(PositionResult.from(position));
  }
}
