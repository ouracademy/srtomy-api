package org.ouracademy.srtomy.organization;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;

import static org.ouracademy.srtomy.person.SystemAccountabilityTypes.*;

@Service
@AllArgsConstructor
public class RoleService {

    AccountabilityTypeRepository accountabilityTypeRepository;
    AccountabilityRepository accountabilityRepository;
    OrganizationRepository organizationRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void sync(Person person, Long locationId, Set<Long> newAccountabilityTypeIds) {
        var accountabilities = accountabilityRepository.findByParentIdAndChildId(locationId, person.getId());
        var oldAccountabilityTypeIds = accountabilities.stream().map(x -> x.getType().getId())
                .collect(Collectors.toSet());

        // location.roles.name not in roles => delete those not in // A - B
        var rolesToDelete = Sets.difference(oldAccountabilityTypeIds, newAccountabilityTypeIds);
        accountabilityRepository.deleteAll(accountabilities.stream()
                .filter(x -> rolesToDelete.contains(x.getType().getId()))
                .toList());

        // new roles not in location.roles => add // B - A
        var rolesToAdd = Sets.difference(newAccountabilityTypeIds, oldAccountabilityTypeIds);
        var location = organizationRepository.getReferenceById(locationId);
        accountabilityRepository
                .saveAll(rolesToAdd.stream().map(typeId -> new Accountability(person, location,
                        accountabilityTypeRepository.getReferenceById(typeId))).toList());
    }

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

    public Stream<AccountabilityType> getRolesOfLocation(Long id) {
        var rolesOfLocation = accountabilityTypeRepository.findByParty(organizationRepository.getReferenceById(id));
        var defaultRoles = getPredefinedRoles();

        return Stream.concat(rolesOfLocation.stream(), defaultRoles.stream());
    }

    public List<AccountabilityType> getPredefinedRoles() {
        return accountabilityTypeRepository.findAllById(List.of(PASTOR, WORSHIP_LEADER, USHER));
    }

    public Stream<AccountabilityType> getRolesOfPerson(Long locationId, Long personId) {
        return accountabilityRepository.findByParentIdAndChildId(locationId, personId).stream().map(x -> x.getType());
    }

}
