package org.ouracademy.ovejafiel.directory.query;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.directory.repository.DirectoryRepository;
import org.ouracademy.ovejafiel.directory.repository.DirectoryRepository.PersonRow;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Component;

/** Shared reading steps of the directory and the public pages. */
@Component
@RequiredArgsConstructor
public class DirectoryReader {

  /** Which people may be named: inside the church only known minors are left out. */
  public enum Audience {
    CHURCH,
    PUBLIC;

    public Predicate<PersonRow> listable(LocalDate today) {
      return switch (this) {
        case CHURCH -> row -> !AgeRules.isKnownMinor(row.age(), row.birthdate(), today);
        case PUBLIC -> row -> AgeRules.isKnownAdult(row.age(), row.birthdate(), today);
      };
    }
  }

  private final DirectoryRepository directoryRepository;

  /**
   * The people who hold a position (cargo) in each node, once each: whoever runs it, from its
   * leaders to its treasurer. Ministerial positions are the only notion of "who is in charge".
   */
  public Map<PartyId, List<String>> teamByNode(Collection<PartyId> nodeIds, Audience audience) {
    return namesByNode(holdersOf(nodeIds), audience);
  }

  /** The people of the node who are not on its team: the team is listed apart. */
  public List<String> membersOf(PartyId nodeId, Audience audience) {
    var team =
        holdersOf(List.of(nodeId)).stream().map(PersonRow::personId).collect(Collectors.toSet());
    var members =
        directoryRepository.findPeople(List.of(nodeId), IS_PART_OF).stream()
            .filter(row -> !team.contains(row.personId()))
            .toList();
    return namesByNode(members, audience).getOrDefault(nodeId, List.of());
  }

  private List<PersonRow> holdersOf(Collection<PartyId> nodeIds) {
    var seen = new java.util.HashSet<List<PartyId>>();
    return directoryRepository.findHeldPositions(nodeIds).stream()
        .filter(held -> seen.add(List.of(held.nodeId(), held.personId())))
        .map(
            held ->
                new PersonRow(
                    held.nodeId(),
                    held.personId(),
                    held.firstName(),
                    held.lastName(),
                    held.age(),
                    held.birthdate()))
        .toList();
  }

  /** Position name to the people who hold it, in the given node. */
  public Map<String, List<String>> positionsOf(PartyId nodeId, Audience audience) {
    var today = LocalDate.now();
    var listable = audience.listable(today);
    return directoryRepository.findHeldPositions(List.of(nodeId)).stream()
        .filter(
            held ->
                listable.test(
                    new PersonRow(
                        held.nodeId(),
                        held.personId(),
                        held.firstName(),
                        held.lastName(),
                        held.age(),
                        held.birthdate())))
        .collect(
            Collectors.groupingBy(
                DirectoryRepository.HeldRow::position,
                java.util.LinkedHashMap::new,
                Collectors.mapping(
                    held -> fullName(held.firstName(), held.lastName()), Collectors.toList())));
  }

  public List<Organization> childrenOf(PartyId nodeId) {
    return directoryRepository.findByParentIdOrderByName(nodeId);
  }

  public List<Organization> nodes(Collection<PartyId> ids) {
    return directoryRepository.findByIdIn(ids);
  }

  private Map<PartyId, List<String>> namesByNode(List<PersonRow> rows, Audience audience) {
    var listable = audience.listable(LocalDate.now());
    return rows.stream()
        .filter(listable)
        .collect(
            Collectors.groupingBy(
                PersonRow::nodeId,
                Collectors.mapping(
                    row -> fullName(row.firstName(), row.lastName()), Collectors.toList())));
  }

  public static String fullName(String firstName, String lastName) {
    return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
  }
}
