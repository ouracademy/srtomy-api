package org.ouracademy.srtomy.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.person.Accountability;
import org.ouracademy.srtomy.person.AccountabilityId;
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityType.Category;
import org.ouracademy.srtomy.person.AccountabilityTypeId;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.PartyId;
import org.ouracademy.srtomy.person.Person;
import org.ouracademy.srtomy.shared.domain.EntityId;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock OrganizationRepository organizationRepository;

  RoleService roleService;

  @BeforeEach
  void setUp() {
    roleService =
        new RoleService(
            accountabilityTypeRepository, accountabilityRepository, organizationRepository);
  }

  private static <T> T withId(T entity, EntityId id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }

  @Test
  void syncRemovesRolesNotInTheNewSetAndAddsTheOnesMissing() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = withId(new Organization(), locationId);
    var person = withId(new Person(), personId);

    var oldRoleTypeId = AccountabilityTypeId.generate();
    var keptRoleTypeId = AccountabilityTypeId.generate();
    var newRoleTypeId = AccountabilityTypeId.generate();
    var oldRoleType =
        withId(new AccountabilityType("USHER", location, Category.ROLE), oldRoleTypeId);
    var keptRoleType =
        withId(new AccountabilityType("PASTOR", location, Category.ROLE), keptRoleTypeId);
    var newRoleType =
        withId(new AccountabilityType("WORSHIP_LEADER", location, Category.ROLE), newRoleTypeId);

    var oldAccountability =
        withId(new Accountability(person, location, oldRoleType), AccountabilityId.generate());
    var keptAccountability =
        withId(new Accountability(person, location, keptRoleType), AccountabilityId.generate());

    when(accountabilityRepository.findByParentIdAndChildId(locationId, personId))
        .thenReturn(List.of(oldAccountability, keptAccountability));
    when(organizationRepository.getReferenceById(locationId)).thenReturn(location);
    when(accountabilityTypeRepository.getReferenceById(newRoleTypeId)).thenReturn(newRoleType);

    roleService.sync(person, locationId, Set.of(keptRoleTypeId, newRoleTypeId));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> deletedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).deleteAll(deletedCaptor.capture());
    assertThat(deletedCaptor.getValue()).containsExactly(oldAccountability);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> savedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).saveAll(savedCaptor.capture());
    assertThat(savedCaptor.getValue()).hasSize(1);
    assertThat(savedCaptor.getValue().get(0).getType()).isEqualTo(newRoleType);
  }

  @Test
  void syncIgnoresAccountabilitiesThatAreNotRoles() {
    var locationId = PartyId.generate();
    var personId = PartyId.generate();
    var location = withId(new Organization(), locationId);
    var person = withId(new Person(), personId);
    var memberProgressType =
        withId(
            new AccountabilityType("BAPTIZED", location, Category.MEMBER_PROGRESS_STATE),
            AccountabilityTypeId.generate());
    var nonRoleAccountability =
        withId(
            new Accountability(person, location, memberProgressType), AccountabilityId.generate());

    when(accountabilityRepository.findByParentIdAndChildId(locationId, personId))
        .thenReturn(List.of(nonRoleAccountability));
    when(organizationRepository.getReferenceById(locationId)).thenReturn(location);

    roleService.sync(person, locationId, Set.of());

    verify(accountabilityRepository).deleteAll(anyList());
    // The non-role accountability must not be touched by the role sync.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> deletedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).deleteAll(deletedCaptor.capture());
    assertThat(deletedCaptor.getValue()).isEmpty();
  }
}
