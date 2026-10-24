package org.ouracademy.srtomy.directory.query;

import java.util.List;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * The basic information of a node, which the church directory always shows: its name, what it is,
 * when it meets and who leads it. {@code memberVisibility} says who sees its people.
 */
public record DirectoryNodeResult(
    PartyId id,
    String name,
    Organization.Type type,
    PartyId parentId,
    String description,
    String schedule,
    MemberVisibility memberVisibility,
    List<String> leaders) {

  public static DirectoryNodeResult of(Organization node, List<String> leaders) {
    return new DirectoryNodeResult(
        node.getId(),
        node.getName(),
        node.getType(),
        node.getParent() == null ? null : node.getParent().getId(),
        node.getDescription(),
        node.getSchedule(),
        node.getMemberVisibility(),
        leaders);
  }
}
