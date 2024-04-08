package org.ouracademy.srtomy.organization;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ouracademy.srtomy.organization.Organization.Type;
import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityRepository;
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

@RestController
@RequestMapping("organizations")
@AllArgsConstructor
@Tag(name = "organizations")
public class OrganizationController {

    OrganizationRepository organizationRepository;
    AccountabilityRepository accountabilityRepository;
    PersonRepository personRepository;

    @GetMapping("{id}/childs")
    public List<Party> findAll(@PathVariable Long id) {
        // TODO: list organizations of a user
        // TODO: add authorization
        return accountabilityRepository.findByParentIdAndTypeIn(id, List.of(Accountability.Type.IS_PART_OF)).stream()
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
    public Organization createLocation(@RequestBody @Valid LocationRequest request) {
        return createOrg(request, Type.LOCATION);
    }

    @GetMapping("/locations/childs/{id}/members")
    public Map<Accountability.Type, Party> getLocationChilds(@PathVariable Long id) {
        return accountabilityRepository
                .findByParentIdAndTypeIn(id, List.of(Accountability.Type.IS_PART_OF, Accountability.Type.LEADER))
                .stream()
                .collect(Collectors.toMap(x -> x.getType(), x -> x.getChild()));
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
                                Accountability.Type.IS_PART_OF)),
                request.leaderIds.stream()
                        .map(id -> new Accountability(
                                personRepository.getReferenceById(id),
                                locationChild,
                                Accountability.Type.LEADER)))
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
            accountabilityRepository.save(
                    new Accountability(organization, parent, Accountability.Type.IS_PART_OF));
        }

        return organization;
    }

    @PutMapping("{id}")
    public ResponseEntity<Organization> edit(@PathVariable Long id,
            @RequestBody @Valid OrganizationRequest organizationRequest) {
        return organizationRepository.findById(id).map(organization -> {
            organization.setName(organizationRequest.name);
            fillInfo(organizationRequest, organization);

            return ResponseEntity.ok(organizationRepository.save(organization));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void fillInfo(OrganizationRequest organizationRequest, Organization organization) {
        organization.setDescription(organizationRequest.description);
        organization.setPhoneNumber(organizationRequest.phoneNumber);
        organization.setAddress(organizationRequest.address);
    }

    @GetMapping("{id}/member-state")
    public List<Accountability> getMemberState(@PathVariable Long id) {
        return accountabilityRepository.findByParentIdAndTypeIn(id, Accountability.MEMBER_ACCOUNTABILITIES);
    }

}
