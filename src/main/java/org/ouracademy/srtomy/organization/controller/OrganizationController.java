package org.ouracademy.srtomy.organization.controller;

import static java.util.stream.Collectors.toMap;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_roles_of_location.GetRolesOfLocationQuery;
import org.ouracademy.srtomy.accountability.query.get_roles_of_location.GetRolesOfLocationQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_roles_of_person.GetRolesOfPersonQuery;
import org.ouracademy.srtomy.accountability.query.get_roles_of_person.GetRolesOfPersonQueryHandler;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommand;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommandHandler;
import org.ouracademy.srtomy.organization.command.create_location.CreateLocationCommand;
import org.ouracademy.srtomy.organization.command.create_location.CreateLocationCommandHandler;
import org.ouracademy.srtomy.organization.command.create_location_child.CreateLocationChildCommand;
import org.ouracademy.srtomy.organization.command.create_location_child.CreateLocationChildCommandHandler;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationCommand;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationCommandHandler;
import org.ouracademy.srtomy.organization.command.edit_organization.EditOrganizationCommand;
import org.ouracademy.srtomy.organization.command.edit_organization.EditOrganizationCommandHandler;
import org.ouracademy.srtomy.organization.command.update_location.UpdateLocationCommand;
import org.ouracademy.srtomy.organization.command.update_location.UpdateLocationCommandHandler;
import org.ouracademy.srtomy.organization.controller.dto.AccountabilityTypeDto;
import org.ouracademy.srtomy.organization.controller.dto.CreateLocationRequest;
import org.ouracademy.srtomy.organization.controller.dto.LocationChildMemberResponse;
import org.ouracademy.srtomy.organization.controller.dto.LocationChildRequest;
import org.ouracademy.srtomy.organization.controller.dto.MemberStateResponse;
import org.ouracademy.srtomy.organization.controller.dto.OrganizationRequest;
import org.ouracademy.srtomy.organization.controller.dto.OrganizationResponse;
import org.ouracademy.srtomy.organization.controller.dto.UpdateLocationRequest;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.organization.query.find_organization_children.FindOrganizationChildrenQuery;
import org.ouracademy.srtomy.organization.query.find_organization_children.FindOrganizationChildrenQueryHandler;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenQuery;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenQueryHandler;
import org.ouracademy.srtomy.organization.query.get_member_state.GetMemberStateQuery;
import org.ouracademy.srtomy.organization.query.get_member_state.GetMemberStateQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter only: translates requests to commands/queries and results to responses. */
@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

  CreateChurchCommandHandler createChurchCommandHandler;
  CreateLocationCommandHandler createLocationCommandHandler;
  UpdateLocationCommandHandler updateLocationCommandHandler;
  CreateLocationChildCommandHandler createLocationChildCommandHandler;
  EditOrganizationCommandHandler editOrganizationCommandHandler;
  DeleteOrganizationCommandHandler deleteOrganizationCommandHandler;
  FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  FindOrganizationChildrenQueryHandler findOrganizationChildrenQueryHandler;
  GetLocationChildrenQueryHandler getLocationChildrenQueryHandler;
  GetMemberStateQueryHandler getMemberStateQueryHandler;
  GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  GetRolesOfLocationQueryHandler getRolesOfLocationQueryHandler;
  GetRolesOfPersonQueryHandler getRolesOfPersonQueryHandler;

  @GetMapping("{id}/childs")
  public List<OrganizationResponse> findAll(@PathVariable UUID id) {
    // TODO: list organizations of a user
    // TODO: add authorization
    return findOrganizationChildrenQueryHandler
        .handle(new FindOrganizationChildrenQuery(new PartyId(id)))
        .stream()
        .map(OrganizationResponse::from)
        .toList();
  }

  @GetMapping("{id}")
  public ResponseEntity<OrganizationResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.of(
        findOrganizationByIdQueryHandler
            .handle(new FindOrganizationByIdQuery(new PartyId(id)))
            .map(OrganizationResponse::from));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse create(@RequestBody @Valid OrganizationRequest request) {
    var result =
        createChurchCommandHandler.handle(
            new CreateChurchCommand(info(request), parentId(request), request.getUserId()));
    return OrganizationResponse.from(result.organization());
  }

  @PostMapping("/locations")
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse createLocation(@RequestBody @Valid CreateLocationRequest request) {
    var result =
        createLocationCommandHandler.handle(
            new CreateLocationCommand(info(request), parentId(request), request.getRoles()));
    return OrganizationResponse.from(result.organization());
  }

  @PutMapping("/locations/{id}")
  public ResponseEntity<OrganizationResponse> updateLocation(
      @PathVariable UUID id, @RequestBody @Valid UpdateLocationRequest request) {
    return ResponseEntity.of(
        updateLocationCommandHandler
            .handle(new UpdateLocationCommand(new PartyId(id), info(request), request.getRoles()))
            .map(result -> OrganizationResponse.from(result.organization())));
  }

  @GetMapping("locations/predefined-roles")
  public List<AccountabilityTypeDto> getPredefinedRoles() {
    return getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery()).stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @GetMapping("locations/{id}/roles")
  public List<AccountabilityTypeDto> getRolesOfLocation(@PathVariable UUID id) {
    return getRolesOfLocationQueryHandler
        .handle(new GetRolesOfLocationQuery(new PartyId(id)))
        .stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @GetMapping("locations/{id}/person/{personId}/roles")
  public List<AccountabilityTypeDto> getRolesOfPerson(
      @PathVariable UUID id, @PathVariable UUID personId) {
    return getRolesOfPersonQueryHandler
        .handle(new GetRolesOfPersonQuery(new PartyId(id), new PartyId(personId)))
        .stream()
        .map(AccountabilityTypeDto::from)
        .toList();
  }

  @GetMapping("/locations/childs/{id}/members")
  public Map<String, List<LocationChildMemberResponse>> getLocationChilds(@PathVariable UUID id) {
    return getLocationChildrenQueryHandler
        .handle(new GetLocationChildrenQuery(new PartyId(id)))
        .childrenByType()
        .entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry ->
                    entry.getValue().stream().map(LocationChildMemberResponse::from).toList()));
  }

  @PostMapping("/locations/childs")
  @ResponseStatus(code = HttpStatus.CREATED)
  public OrganizationResponse createLocationChild(
      @RequestBody @Valid LocationChildRequest request) {
    var result =
        createLocationChildCommandHandler.handle(
            new CreateLocationChildCommand(
                info(request),
                parentId(request),
                request.getType(),
                request.getMemberIds().stream().map(PartyId::new).toList(),
                request.getLeaderIds().stream().map(PartyId::new).toList()));
    return OrganizationResponse.from(result.organization());
  }

  @PutMapping("{id}")
  public ResponseEntity<OrganizationResponse> edit(
      @PathVariable UUID id, @RequestBody @Valid OrganizationRequest request) {
    return ResponseEntity.of(
        editOrganizationCommandHandler
            .handle(new EditOrganizationCommand(new PartyId(id), info(request)))
            .map(result -> OrganizationResponse.from(result.organization())));
  }

  @GetMapping("{id}/member-state")
  public List<MemberStateResponse> getMemberState(@PathVariable UUID id) {
    return getMemberStateQueryHandler.handle(new GetMemberStateQuery(new PartyId(id))).stream()
        .map(MemberStateResponse::from)
        .toList();
  }

  @DeleteMapping("{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    var result =
        deleteOrganizationCommandHandler.handle(new DeleteOrganizationCommand(new PartyId(id)));
    return switch (result.outcome()) {
      case DELETED -> ResponseEntity.noContent().build();
      case NOT_FOUND -> ResponseEntity.notFound().build();
      case NOT_DELETABLE -> ResponseEntity.badRequest().build();
    };
  }

  private static OrganizationInfo info(OrganizationRequest request) {
    return new OrganizationInfo(
        request.getName(),
        request.getDescription(),
        request.getAddress(),
        request.getPhoneNumber());
  }

  private static PartyId parentId(OrganizationRequest request) {
    return request.getParentId() == null ? null : new PartyId(request.getParentId());
  }
}
