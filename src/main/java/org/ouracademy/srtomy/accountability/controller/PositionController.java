package org.ouracademy.srtomy.accountability.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.delete_position.DeletePositionCommand;
import org.ouracademy.srtomy.accountability.command.delete_position.DeletePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.rename_position.RenamePositionCommand;
import org.ouracademy.srtomy.accountability.command.rename_position.RenamePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.set_position_access.SetPositionAccessCommand;
import org.ouracademy.srtomy.accountability.command.set_position_access.SetPositionAccessCommandHandler;
import org.ouracademy.srtomy.accountability.controller.dto.CreatePositionRequest;
import org.ouracademy.srtomy.accountability.controller.dto.PositionAccessRequest;
import org.ouracademy.srtomy.accountability.controller.dto.PositionRequest;
import org.ouracademy.srtomy.accountability.controller.dto.PositionResponse;
import org.ouracademy.srtomy.accountability.controller.dto.PositionSuggestionResponse;
import org.ouracademy.srtomy.accountability.controller.dto.PositionSummaryResponse;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQuery;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.GetPositionSuggestionsQuery;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.GetPositionSuggestionsQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_positions_of_church.GetPositionsOfChurchQuery;
import org.ouracademy.srtomy.accountability.query.get_positions_of_church.GetPositionsOfChurchQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_positions_of_person.GetPositionsOfPersonQuery;
import org.ouracademy.srtomy.accountability.query.get_positions_of_person.GetPositionsOfPersonQueryHandler;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ministerial positions (cargos): names each church picks its own way. A position may carry an
 * access level; whoever holds it (through a person linked to their user) gets that level's
 * permissions. Managing positions needs {@code positions.manage} on the church, and giving or
 * changing a level is further checked against the access of the user who asks.
 */
@RestController
@AllArgsConstructor
@Tag(name = "positions")
public class PositionController {

  GetPositionsOfChurchQueryHandler getPositionsOfChurchQueryHandler;
  GetPositionsOfPersonQueryHandler getPositionsOfPersonQueryHandler;
  GetPositionSuggestionsQueryHandler getPositionSuggestionsQueryHandler;
  FindPositionByIdQueryHandler findPositionByIdQueryHandler;
  CreatePositionCommandHandler createPositionCommandHandler;
  RenamePositionCommandHandler renamePositionCommandHandler;
  DeletePositionCommandHandler deletePositionCommandHandler;
  SetPositionAccessCommandHandler setPositionAccessCommandHandler;
  FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /** The positions of the church that {@code id} (any node) belongs to. */
  @GetMapping("organizations/{id}/positions")
  public List<PositionSummaryResponse> findAll(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.ORG_READ);
    return getPositionsOfChurchQueryHandler
        .handle(new GetPositionsOfChurchQuery(churchOf(organizationId)))
        .stream()
        .map(PositionSummaryResponse::from)
        .toList();
  }

  @PostMapping("organizations/{id}/positions")
  @ResponseStatus(HttpStatus.CREATED)
  public PositionResponse create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid CreatePositionRequest request) {
    var churchId = churchOf(new PartyId(id));
    authorize(jwt, churchId, Permission.POSITIONS_MANAGE);
    return PositionResponse.from(
        createPositionCommandHandler
            .handle(
                new CreatePositionCommand(
                    jwt.getSubject(),
                    churchId,
                    request.name(),
                    roleIds(request.roleIds()),
                    permissions(request.permissions())))
            .position());
  }

  @PutMapping("positions/{id}")
  public ResponseEntity<PositionResponse> rename(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid PositionRequest request) {
    var positionId = new AccountabilityTypeId(id);
    authorizeOnPosition(jwt, positionId);
    return ResponseEntity.of(
        renamePositionCommandHandler
            .handle(new RenamePositionCommand(positionId, request.name()))
            .map(result -> PositionResponse.from(result.position())));
  }

  /** Changes what a position gives; no roles and no permissions make it a plain label. */
  @PutMapping("positions/{id}/access")
  public ResponseEntity<PositionResponse> setAccess(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody PositionAccessRequest request) {
    var positionId = new AccountabilityTypeId(id);
    authorizeOnPosition(jwt, positionId);
    return ResponseEntity.of(
        setPositionAccessCommandHandler
            .handle(
                new SetPositionAccessCommand(
                    jwt.getSubject(),
                    positionId,
                    roleIds(request.roleIds()),
                    permissions(request.permissions())))
            .map(result -> PositionResponse.from(result.position())));
  }

  @DeleteMapping("positions/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var positionId = new AccountabilityTypeId(id);
    authorizeOnPosition(jwt, positionId);
    deletePositionCommandHandler.handle(new DeletePositionCommand(jwt.getSubject(), positionId));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("positions/suggestions")
  public List<PositionSuggestionResponse> suggestions() {
    return getPositionSuggestionsQueryHandler
        .handle(new GetPositionSuggestionsQuery())
        .suggestions()
        .stream()
        .map(PositionSuggestionResponse::from)
        .toList();
  }

  @GetMapping("organizations/{id}/people/{personId}/positions")
  public List<PositionResponse> findOfPerson(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID personId) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.MEMBERS_READ);
    return getPositionsOfPersonQueryHandler
        .handle(new GetPositionsOfPersonQuery(organizationId, new PartyId(personId)))
        .stream()
        .map(PositionResponse::from)
        .toList();
  }

  private static Set<IamRoleId> roleIds(List<UUID> ids) {
    return ids == null ? Set.of() : ids.stream().map(IamRoleId::new).collect(Collectors.toSet());
  }

  private static Set<String> permissions(List<String> values) {
    return values == null ? Set.of() : Set.copyOf(values);
  }

  private void authorize(Jwt jwt, PartyId organizationId, Permission permission) {
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }

  /** 404 when the position does not exist, 403 without {@code positions.manage} on its church. */
  private void authorizeOnPosition(Jwt jwt, AccountabilityTypeId positionId) {
    var position =
        findPositionByIdQueryHandler
            .handle(new FindPositionByIdQuery(positionId))
            .orElseThrow(() -> new NoSuchElementException("Position not found"));
    authorize(jwt, position.churchId(), Permission.POSITIONS_MANAGE);
  }

  private PartyId churchOf(PartyId organizationId) {
    return findChurchOfOrganizationQueryHandler
        .handle(new FindChurchOfOrganizationQuery(organizationId))
        .orElseThrow(() -> new NoSuchElementException("Organization not found"))
        .churchId();
  }
}
