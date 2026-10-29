package org.ouracademy.ovejafiel.person.query.find_person_matches;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesResult.Match;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPersonMatchesQueryHandlerImpl implements FindPersonMatchesQueryHandler {

  static final int MIN_NAME_LENGTH = 3;
  static final int MAX_MATCHES = 5;

  private final PersonRepository personRepository;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public FindPersonMatchesResult handle(FindPersonMatchesQuery query) {
    var name = query.name() == null ? "" : query.name().trim();
    var email = query.email() == null ? "" : query.email().trim();
    if (name.length() < MIN_NAME_LENGTH) {
      name = "";
    }
    if (name.isEmpty() && email.isEmpty()) {
      return new FindPersonMatchesResult(java.util.List.of());
    }
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(query.organizationId()))
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Organization not found: " + query.organizationId().value()))
            .churchId();
    var organizationIds =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(churchId))
            .ids()
            .stream()
            .map(PartyId::value)
            .toList();
    var matches =
        personRepository.findMatches(name, email, organizationIds, MAX_MATCHES).stream()
            .map(
                row ->
                    new Match(
                        new PartyId(row.getId()),
                        row.getFullName(),
                        row.getEmail(),
                        row.getPhoneNumber(),
                        Boolean.TRUE.equals(row.getActive()),
                        row.getRemovedTime()))
            .toList();
    return new FindPersonMatchesResult(matches);
  }
}
