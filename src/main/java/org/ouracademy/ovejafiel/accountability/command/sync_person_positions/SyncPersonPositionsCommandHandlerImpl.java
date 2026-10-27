package org.ouracademy.ovejafiel.accountability.command.sync_person_positions;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncPersonPositionsCommandHandlerImpl implements SyncPersonPositionsCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;
  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public SyncPersonPositionsResult handle(SyncPersonPositionsCommand command) {
    var held =
        accountabilityRepository
            .findByParentIdAndChildId(command.organizationId(), command.personId())
            .stream()
            .filter(accountability -> accountability.getType().isPosition())
            .toList();
    var wanted =
        command.positionIds().isEmpty()
            ? List.<AccountabilityType>of()
            : accountabilityTypeRepository.findByIdInAndPartyIdAndCategory(
                command.positionIds(), command.churchId(), Category.POSITION);
    var heldIds = held.stream().map(a -> a.getType().getId()).collect(Collectors.toSet());
    var wantedIds = wanted.stream().map(AccountabilityType::getId).collect(Collectors.toSet());

    var toRemove = held.stream().filter(a -> !wantedIds.contains(a.getType().getId())).toList();
    var idsToAdd = Sets.difference(wantedIds, heldIds);

    // Before changing anything: giving or taking a position that gives access is not for everyone.
    Stream.concat(
            toRemove.stream().map(Accountability::getType),
            wanted.stream().filter(type -> idsToAdd.contains(type.getId())))
        .filter(AccountabilityType::hasAccess)
        .forEach(
            type ->
                authorizeAccessGrantQueryHandler.handle(
                    new AuthorizeAccessGrantQuery(
                        command.subject(),
                        type.getRoleIds(),
                        type.getPermissions(),
                        command.organizationId())));

    accountabilityRepository.deleteAll(toRemove);
    var person = entityManager.getReference(Party.class, command.personId());
    var organization = entityManager.getReference(Party.class, command.organizationId());
    var toAdd =
        wanted.stream()
            .filter(type -> idsToAdd.contains(type.getId()))
            .map(type -> new Accountability(person, organization, type))
            .toList();
    accountabilityRepository.saveAll(toAdd);

    return new SyncPersonPositionsResult(toAdd.size(), toRemove.size());
  }
}
