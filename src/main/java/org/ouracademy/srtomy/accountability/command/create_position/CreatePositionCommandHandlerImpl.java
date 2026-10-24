package org.ouracademy.srtomy.accountability.command.create_position;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.PositionNames;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.Party;
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
