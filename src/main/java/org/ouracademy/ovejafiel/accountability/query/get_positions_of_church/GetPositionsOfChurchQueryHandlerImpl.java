package org.ouracademy.ovejafiel.accountability.query.get_positions_of_church;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.query.PositionHolderResult;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.PositionSummaryResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository.PositionHolder;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPositionsOfChurchQueryHandlerImpl implements GetPositionsOfChurchQueryHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;
  private final ExpandPermissionsQueryHandler expandPermissionsQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<PositionSummaryResult> handle(GetPositionsOfChurchQuery query) {
    var positions =
        accountabilityTypeRepository.findByPartyIdAndCategoryOrderByName(
            query.churchId(), Category.POSITION);
    if (positions.isEmpty()) {
      return List.of();
    }
    Map<UUID, List<PositionHolderResult>> holders =
        accountabilityRepository
            .findHoldersOf(positions.stream().map(p -> p.getId().value()).toList())
            .stream()
            .collect(
                Collectors.groupingBy(
                    PositionHolder::getPositionId,
                    Collectors.mapping(
                        holder ->
                            new PositionHolderResult(
                                new PartyId(holder.getPersonId()),
                                holder.getName(),
                                holder.getOrganizationName(),
                                holder.getEmail()),
                        Collectors.toList())));
    return positions.stream()
        .map(
            position ->
                new PositionSummaryResult(
                    PositionResult.from(position),
                    expandPermissionsQueryHandler
                        .handle(
                            new ExpandPermissionsQuery(
                                position.getRoleIds(), position.getPermissions()))
                        .permissions(),
                    holders.getOrDefault(position.getId().value(), List.of())))
        .toList();
  }
}
