package org.ouracademy.ovejafiel.organization.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.GetMemberGrowthQuery;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.GetMemberGrowthQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.GetMemberGrowthForRangeQuery;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth_for_range.GetMemberGrowthForRangeQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand.Creator;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand.PositionSeed;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommandHandler;
import org.ouracademy.ovejafiel.organization.command.create_location.CreateLocationCommand;
import org.ouracademy.ovejafiel.organization.command.create_location.CreateLocationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.create_location_child.CreateLocationChildCommand;
import org.ouracademy.ovejafiel.organization.command.create_location_child.CreateLocationChildCommandHandler;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationCommand;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.edit_organization.EditOrganizationCommand;
import org.ouracademy.ovejafiel.organization.command.edit_organization.EditOrganizationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.update_location.UpdateLocationCommand;
import org.ouracademy.ovejafiel.organization.command.update_location.UpdateLocationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.update_location_child.UpdateLocationChildCommand;
import org.ouracademy.ovejafiel.organization.command.update_location_child.UpdateLocationChildCommandHandler;
import org.ouracademy.ovejafiel.organization.controller.dto.CreateChurchRequest;
import org.ouracademy.ovejafiel.organization.controller.dto.CreateLocationRequest;
import org.ouracademy.ovejafiel.organization.controller.dto.LocationChildRequest;
import org.ouracademy.ovejafiel.organization.controller.dto.MemberGrowthRangeResponse;
import org.ouracademy.ovejafiel.organization.controller.dto.MemberGrowthResponse;
import org.ouracademy.ovejafiel.organization.controller.dto.OrganizationRequest;
import org.ouracademy.ovejafiel.organization.controller.dto.OrganizationResponse;
import org.ouracademy.ovejafiel.organization.controller.dto.SocialLinksRequest;
import org.ouracademy.ovejafiel.organization.controller.dto.UpdateLocationChildRequest;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_children.FindOrganizationChildrenQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_children.FindOrganizationChildrenQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.DateRange;
import org.ouracademy.ovejafiel.shared.TokenClaims;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter only: translates requests to commands/queries and results to responses. Every
 * endpoint but church creation first checks the caller's permission on the node it acts on.
 */
@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

  CreateChurchCommandHandler createChurchCommandHandler;
  CreateLocationCommandHandler createLocationCommandHandler;
  UpdateLocationCommandHandler updateLocationCommandHandler;
  CreateLocationChildCommandHandler createLocationChildCommandHandler;
  UpdateLocationChildCommandHandler updateLocationChildCommandHandler;
  EditOrganizationCommandHandler editOrganizationCommandHandler;
  DeleteOrganizationCommandHandler deleteOrganizationCommandHandler;
  FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  FindOrganizationChildrenQueryHandler findOrganizationChildrenQueryHandler;
  GetMemberGrowthQueryHandler getMemberGrowthQueryHandler;
  GetMemberGrowthForRangeQueryHandler getMemberGrowthForRangeQueryHandler;
  FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /**
   * Lists the nodes directly under a node: the locations of a church, or the groups and ministries
   * of a location, group or ministry. Needs {@code org.read}.
   *
   * @param id Id of the node: a church, location, group or ministry.
   */
  @GetMapping("{id}/childs")
  public List<OrganizationResponse> findAll(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    authorize(jwt, id, Permission.ORG_READ);
    return findOrganizationChildrenQueryHandler
        .handle(new FindOrganizationChildrenQuery(new PartyId(id)))
        .stream()
        .map(OrganizationResponse::from)
        .toList();
  }

  /**
   * Gets a node (church, location, group or ministry). Returns its name, type, description,
   * address, phone, links and schedule. Needs {@code org.read}.
   *
   * @param id Id of the node: a church, location, group or ministry.
   */
  @GetMapping("{id}")
  public ResponseEntity<OrganizationResponse> findById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    authorize(jwt, id, Permission.ORG_READ);
    return ResponseEntity.of(
        findOrganizationByIdQueryHandler
            .handle(new FindOrganizationByIdQuery(new PartyId(id)))
            .map(OrganizationResponse::from));
  }

  /**
   * Creates a church with its first location and its initial positions. Any signed-in user may;
   * they become its Owner.
   */
  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateChurchRequest request) {
    var location = request.location();
    var result =
        createChurchCommandHandler.handle(
            new CreateChurchCommand(
                new OrganizationInfo(
                    request.name(),
                    request.description(),
                    request.address(),
                    request.phoneNumber(),
                    SocialLinksRequest.toDomain(request.socialLinks())),
                new OrganizationInfo(
                    location.name(),
                    location.description(),
                    location.address(),
                    location.phoneNumber(),
                    SocialLinksRequest.toDomain(location.socialLinks())),
                request.positions().stream()
                    .map(
                        position ->
                            new PositionSeed(
                                position.name(),
                                position.roleIds().stream()
                                    .map(IamRoleId::new)
                                    .collect(Collectors.toSet()),
                                Set.copyOf(position.permissions())))
                    .toList(),
                new Creator(
                    request.creator().firstName(),
                    request.creator().lastName(),
                    request.creator().positionName()),
                jwt.getSubject(),
                TokenClaims.email(jwt),
                TokenClaims.emailVerified(jwt)));
    return OrganizationResponse.from(result.organization());
  }

  /** Creates a location (sede) under a church. Needs {@code org.create_child} on the church. */
  @PostMapping("/locations")
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse createLocation(
      @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateLocationRequest request) {
    authorize(jwt, request.getParentId(), Permission.ORG_CREATE_CHILD);
    var result =
        createLocationCommandHandler.handle(
            new CreateLocationCommand(info(request), parentId(request)));
    return OrganizationResponse.from(result.organization());
  }

  /**
   * Changes a location. Send the whole node, not only what changes: a text field left out is
   * cleared. Needs {@code org.update} on it.
   *
   * @param id Id of the location.
   */
  @PutMapping("/locations/{id}")
  public ResponseEntity<OrganizationResponse> updateLocation(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid OrganizationRequest request) {
    authorize(jwt, id, Permission.ORG_UPDATE);
    return ResponseEntity.of(
        updateLocationCommandHandler
            .handle(new UpdateLocationCommand(new PartyId(id), info(request)))
            .map(result -> OrganizationResponse.from(result.organization())));
  }

  /**
   * Creates a group or ministry under a location, group or ministry. It may start with some people
   * as members. Needs {@code org.create_child} on the parent.
   */
  @PostMapping("/locations/childs")
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse createLocationChild(
      @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid LocationChildRequest request) {
    authorize(jwt, request.getParentId(), Permission.ORG_CREATE_CHILD);
    var result =
        createLocationChildCommandHandler.handle(
            new CreateLocationChildCommand(
                info(request),
                parentId(request),
                request.getType(),
                request.getMemberIds().stream().map(PartyId::new).toList()));
    return OrganizationResponse.from(result.organization());
  }

  /**
   * Changes a group or ministry. Send the whole node, not only what changes: a text field left out
   * is cleared. Needs {@code org.update} on it.
   *
   * @param id Id of the group or ministry.
   */
  @PutMapping("/locations/childs/{id}")
  public ResponseEntity<OrganizationResponse> updateLocationChild(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid UpdateLocationChildRequest request) {
    authorize(jwt, id, Permission.ORG_UPDATE);
    return ResponseEntity.of(
        updateLocationChildCommandHandler
            .handle(
                new UpdateLocationChildCommand(new PartyId(id), info(request), request.getType()))
            .map(result -> OrganizationResponse.from(result.organization())));
  }

  /**
   * Changes a church. Send the whole node, not only what changes: a text field left out is cleared.
   * Needs {@code org.update} on it.
   *
   * @param id Id of the church.
   */
  @PutMapping("{id}")
  public ResponseEntity<OrganizationResponse> edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid OrganizationRequest request) {
    authorize(jwt, id, Permission.ORG_UPDATE);
    return ResponseEntity.of(
        editOrganizationCommandHandler
            .handle(new EditOrganizationCommand(new PartyId(id), info(request)))
            .map(result -> OrganizationResponse.from(result.organization())));
  }

  /**
   * How the people of a node and of everything below it are doing over the last months. Says how
   * many are visitors, in progress or members now and, month by month, how many joined, progressed
   * or left. Needs {@code members.read}.
   *
   * @param id Id of the node: a church, location, group or ministry.
   * @param months How many months back, from 1 to 36. 12 when omitted.
   */
  @GetMapping("{id}/growth")
  public MemberGrowthResponse growth(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "12") int months) {
    authorize(jwt, id, Permission.MEMBERS_READ);
    var subtree =
        findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(new PartyId(id)));
    return MemberGrowthResponse.from(
        getMemberGrowthQueryHandler.handle(
            new GetMemberGrowthQuery(subtree.ids(), Math.clamp(months, 1, 36))));
  }

  /**
   * How the people of a node and of everything below it changed over a range of dates. Like the
   * growth by months, but grouped by day (ranges up to a month), week (up to about four months) or
   * month. Needs {@code members.read}.
   *
   * @param id Id of the node: a church, location, group or ministry.
   * @param startDate First day of the range, as an ISO date (2026-01-01).
   * @param endDate Last day of the range, as an ISO date (2026-03-31).
   */
  @GetMapping("{id}/growth-range")
  public MemberGrowthRangeResponse growthRange(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    authorize(jwt, id, Permission.MEMBERS_READ);
    var range = new DateRange(startDate, endDate);
    var subtree =
        findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(new PartyId(id)));
    return MemberGrowthRangeResponse.from(
        getMemberGrowthForRangeQueryHandler.handle(
            new GetMemberGrowthForRangeQuery(
                subtree.ids(), range.start(), range.end(), range.bucket())));
  }

  /**
   * Deletes a node; it cannot be undone. 400 when the node cannot be deleted. Needs {@code
   * org.delete} on it.
   *
   * @param id Id of the node: a church, location, group or ministry.
   */
  @DeleteMapping("{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    authorize(jwt, id, Permission.ORG_DELETE);
    var result =
        deleteOrganizationCommandHandler.handle(new DeleteOrganizationCommand(new PartyId(id)));
    return switch (result.outcome()) {
      case DELETED -> ResponseEntity.noContent().build();
      case NOT_FOUND -> ResponseEntity.notFound().build();
      case NOT_DELETABLE -> ResponseEntity.badRequest().build();
    };
  }

  private void authorize(Jwt jwt, UUID organizationId, Permission permission) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(jwt.getSubject(), new PartyId(organizationId), permission));
  }

  private static OrganizationInfo info(OrganizationRequest request) {
    return new OrganizationInfo(
        request.getName(),
        request.getDescription(),
        request.getAddress(),
        request.getPhoneNumber(),
        SocialLinksRequest.toDomain(request.getSocialLinks()),
        request.getSchedule(),
        request.getMemberVisibility(),
        request.getShowOrgChart());
  }

  private static PartyId parentId(OrganizationRequest request) {
    return request.getParentId() == null ? null : new PartyId(request.getParentId());
  }
}
