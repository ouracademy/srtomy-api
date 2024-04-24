package org.ouracademy.srtomy.organization;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class RoleService {

    AccountabilityTypeRepository accountabilityTypeRepository;
    AccountabilityRepository accountabilityRepository;
    OrganizationRepository organizationRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void sync(Organization location, SortedSet<String> roles) {
        var locationRoles = accountabilityTypeRepository.findByParty(location);
        var locationRoleNames = locationRoles.stream().map(x -> x.getName()).collect(Collectors.toSet());

        // location.roles.name not in roles => delete those not in // A - B
        var rolesToDelete = Sets.difference(locationRoleNames, roles);
        for (String roleNameToDelete : rolesToDelete) {
            var roleToDelete = locationRoles.stream()
                    .filter(x -> x.getName().equals(roleNameToDelete)).findFirst()
                    .orElseThrow();
            accountabilityTypeRepository.delete(roleToDelete);
            accountabilityRepository.deleteAllByType(roleToDelete);
        }

        // new roles not in location.roles => add // B - A
        var rolesToAdd = Sets.difference(roles, locationRoleNames);
        accountabilityTypeRepository
                .saveAll(rolesToAdd.stream().map(roleName -> new AccountabilityType(roleName, location)).toList());
    }

    public List<AccountabilityType> getRolesOfLocation(Long id) {
        var rolesOfLocation = accountabilityTypeRepository.findByParty(organizationRepository.getReferenceById(id));
        var defaultRoles = accountabilityTypeRepository.findAllById(List.of());

        return Stream.concat(rolesOfLocation.stream(), defaultRoles.stream()).toList();
    }

}
