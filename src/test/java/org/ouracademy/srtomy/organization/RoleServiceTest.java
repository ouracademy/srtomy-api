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
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityType.Category;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.Person;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

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

  private static <T> T withId(T entity, Long id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }

  @Test
  void syncRemovesRolesNotInTheNewSetAndAddsTheOnesMissing() {
    var location = withId(new Organization(), 1L);
    var person = withId(new Person(), 2L);

    var oldRoleType = withId(new AccountabilityType("USHER", location, Category.ROLE), 10L);
    var keptRoleType = withId(new AccountabilityType("PASTOR", location, Category.ROLE), 20L);
    var newRoleType =
        withId(new AccountabilityType("WORSHIP_LEADER", location, Category.ROLE), 30L);

    var oldAccountability = withId(new Accountability(person, location, oldRoleType), 100L);
    var keptAccountability = withId(new Accountability(person, location, keptRoleType), 101L);

    when(accountabilityRepository.findByParentIdAndChildId(1L, 2L))
        .thenReturn(List.of(oldAccountability, keptAccountability));
    when(organizationRepository.getReferenceById(1L)).thenReturn(location);
    when(accountabilityTypeRepository.getReferenceById(30L)).thenReturn(newRoleType);

    roleService.sync(person, 1L, Set.of(20L, 30L));

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
    var location = withId(new Organization(), 1L);
    var person = withId(new Person(), 2L);
    var memberProgressType =
        withId(new AccountabilityType("BAPTIZED", location, Category.MEMBER_PROGRESS_STATE), 40L);
    var nonRoleAccountability =
        withId(new Accountability(person, location, memberProgressType), 200L);

    when(accountabilityRepository.findByParentIdAndChildId(1L, 2L))
        .thenReturn(List.of(nonRoleAccountability));
    when(organizationRepository.getReferenceById(1L)).thenReturn(location);

    roleService.sync(person, 1L, Set.of());

    verify(accountabilityRepository).deleteAll(anyList());
    // The non-role accountability must not be touched by the role sync.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Accountability>> deletedCaptor = ArgumentCaptor.forClass(List.class);
    verify(accountabilityRepository).deleteAll(deletedCaptor.capture());
    assertThat(deletedCaptor.getValue()).isEmpty();
  }
}
