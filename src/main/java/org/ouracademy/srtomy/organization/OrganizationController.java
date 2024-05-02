package org.ouracademy.srtomy.organization;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.MEMBER_ACCOUNTABILITIES;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.ouracademy.srtomy.organization.Organization.Type;
import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.Party;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

    OrganizationRepository organizationRepository;
    AccountabilityRepository accountabilityRepository;
    PersonRepository personRepository;
    RoleService roleService;
    AccountabilityTypeRepository accountabilityTypeRepository;

    @GetMapping("{id}/childs")
    public List<Party> findAll(@PathVariable Long id) {
        // TODO: list organizations of a user
        // TODO: add authorization

        return accountabilityRepository.findByParentIdAndTypeIn(id, types(IS_PART_OF))
                .stream()
                .map(x -> x.getChild())
                .toList();
    }

    @GetMapping("{id}")
    public ResponseEntity<Organization> findById(@PathVariable Long id) {
        return ResponseEntity.of(organizationRepository.findById(id));
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public Organization create(@RequestBody @Valid OrganizationRequest request) {
        return createOrg(request, Type.CHURCH);
    }

    @PostMapping("/locations")
    @ResponseStatus(code = HttpStatus.CREATED)
    @Transactional
    public Organization createLocation(@RequestBody @Valid CreateLocationRequest request) {
        Organization location = createOrg(request, Type.LOCATION);
        roleService.sync(location, request.roles);
        return location;
    }

    @PutMapping("/locations/{id}")
    @Transactional
    public ResponseEntity<Organization> updateLocation(@PathVariable Long id,
            @RequestBody @Valid UpdateLocationRequest request) {
        return editOrgInfo(id, request).map(org -> {
            roleService.sync(org, request.roles);
            return ResponseEntity.ok(org);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("locations/predefined-roles")
    public List<AccountabilityTypeDto> getPredefinedRoles() {
        return roleService.getPredefinedRoles()
                .stream()
                .map(x -> new AccountabilityTypeDto(x.getId(), x.getName(), x.isPredefined())).toList();
    }

    @GetMapping("locations/{id}/roles")
    public List<AccountabilityTypeDto> getRolesOfLocation(@PathVariable Long id) {
        return roleService.getRolesOfLocation(id)
                .map(x -> new AccountabilityTypeDto(x.getId(), x.getName(), x.isPredefined())).toList();
    }

    @Getter
    @AllArgsConstructor
    public static class AccountabilityTypeDto {
        Long id;
        String name;
        Boolean isPredefined;
    }

    @GetMapping("/locations/childs/{id}/members")
    public Map<String, List<Party>> getLocationChilds(@PathVariable Long id) {
        return accountabilityRepository
                .findByParentIdAndTypeIn(id,
                        types(IS_PART_OF, LEADER))
                .stream()
                .collect(
                        groupingBy(
                                x -> x.getType().getName(),
                                mapping(x -> x.getChild(), toList())));
    }

    @PostMapping("/locations/childs")
    @ResponseStatus(code = HttpStatus.CREATED)
    @Transactional
    public Organization createLocationChild(@RequestBody @Valid LocationChildRequest request) {
        Organization locationChild = createOrg(request, request.type);
        var accountabilities = Stream.concat(
                request.memberIds.stream()
                        .map(id -> new Accountability(
                                personRepository.getReferenceById(id),
                                locationChild,
                                accountabilityTypeRepository.getReferenceById(IS_PART_OF))),
                request.leaderIds.stream()
                        .map(id -> new Accountability(
                                personRepository.getReferenceById(id),
                                locationChild,
                                accountabilityTypeRepository.getReferenceById(LEADER))))
                .toList();
        accountabilityRepository.saveAll(accountabilities);
        return locationChild;
    }

    @Transactional
    public Organization createOrg(OrganizationRequest organizationRequest, Type type) {
        var organization = new Organization(organizationRequest.name);
        fillInfo(organizationRequest, organization);
        organization.type = type;
        organization = organizationRepository.save(organization);

        if (organizationRequest.parentId != null) {
            var parent = organizationRepository.getReferenceById(organizationRequest.parentId);
            var accountabilityType = accountabilityTypeRepository
                    .getReferenceById(IS_PART_OF);

            accountabilityRepository.save(new Accountability(organization, parent, accountabilityType));
        }

        return organization;
    }

    @PutMapping("{id}")
    public ResponseEntity<Organization> edit(@PathVariable Long id,
            @RequestBody @Valid OrganizationRequest organizationRequest) {
        return editOrgInfo(id, organizationRequest)
                .map(org -> ResponseEntity.ok(org))
                .orElse(ResponseEntity.notFound().build());
    }

    private Optional<Organization> editOrgInfo(Long id, OrganizationRequest organizationRequest) {
        return organizationRepository.findById(id).map(organization -> {
            organization.setName(organizationRequest.name);
            fillInfo(organizationRequest, organization);

            return organizationRepository.save(organization);
        });
    }

    private void fillInfo(OrganizationRequest organizationRequest, Organization organization) {
        organization.setDescription(organizationRequest.description);
        organization.setPhoneNumber(organizationRequest.phoneNumber);
        organization.setAddress(organizationRequest.address);
    }

    @GetMapping("{id}/member-state")
    public List<Accountability> getMemberState(@PathVariable Long id) {
        return accountabilityRepository.findByParentIdAndTypeIn(id, types(MEMBER_ACCOUNTABILITIES));
    }

    private List<AccountabilityType> types(Long... ids) {
        return List.of(ids).stream().map(x -> accountabilityTypeRepository.getReferenceById(x)).toList();
    }

}
