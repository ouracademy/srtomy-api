package org.ouracademy.ovejafiel.person.query.find_person_matches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.person.repository.PersonRepository.PersonMatch;

@ExtendWith(MockitoExtension.class)
class FindPersonMatchesQueryHandlerImplTests {

  @Mock PersonRepository personRepository;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  FindPersonMatchesQueryHandlerImpl handler;

  PartyId groupId = PartyId.generate();
  PartyId churchId = PartyId.generate();

  @BeforeEach
  void setUp() {
    handler =
        new FindPersonMatchesQueryHandlerImpl(
            personRepository,
            findChurchOfOrganizationQueryHandler,
            findOrganizationSubtreeQueryHandler);
  }

  @Test
  void handleSearchesTheWholeChurchEvenWhenAskedFromAGroup() {
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    when(findOrganizationSubtreeQueryHandler.handle(new FindOrganizationSubtreeQuery(churchId)))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(churchId, groupId)));
    var removedAt = LocalDateTime.of(2026, 1, 2, 3, 4);
    var activeId = UUID.randomUUID();
    var removedId = UUID.randomUUID();
    when(personRepository.findMatches(
            eq("Ana Diaz"), eq(""), eq(List.of(churchId.value(), groupId.value())), anyInt()))
        .thenReturn(
            List.of(
                row(activeId, "Ana Diaz", true, null),
                row(removedId, "Ana Diaz", false, removedAt)));

    var result = handler.handle(new FindPersonMatchesQuery(groupId, " Ana Diaz ", null));

    assertThat(result.matches()).hasSize(2);
    assertThat(result.matches().get(0).id()).isEqualTo(new PartyId(activeId));
    assertThat(result.matches().get(0).active()).isTrue();
    assertThat(result.matches().get(1).active()).isFalse();
    assertThat(result.matches().get(1).removedTime()).isEqualTo(removedAt);
  }

  @Test
  void handleIgnoresANameTooShortToBeUsefulButStillMatchesByEmail() {
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    when(findOrganizationSubtreeQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(churchId)));
    when(personRepository.findMatches(eq(""), eq("ana@x.org"), any(), anyInt()))
        .thenReturn(List.of());

    var result = handler.handle(new FindPersonMatchesQuery(churchId, "An", "ana@x.org"));

    assertThat(result.matches()).isEmpty();
    verify(personRepository).findMatches(eq(""), eq("ana@x.org"), any(), anyInt());
  }

  @Test
  void handleWithNothingToMatchOnDoesNotQuery() {
    var result = handler.handle(new FindPersonMatchesQuery(groupId, "  ", null));

    assertThat(result.matches()).isEmpty();
    verifyNoInteractions(personRepository, findChurchOfOrganizationQueryHandler);
  }

  private static PersonMatch row(UUID id, String name, boolean active, LocalDateTime removedTime) {
    return new PersonMatch() {
      public UUID getId() {
        return id;
      }

      public String getFullName() {
        return name;
      }

      public String getEmail() {
        return null;
      }

      public String getPhoneNumber() {
        return null;
      }

      public Boolean getActive() {
        return active;
      }

      public LocalDateTime getRemovedTime() {
        return removedTime;
      }
    };
  }
}
