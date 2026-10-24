package org.ouracademy.srtomy.directory.query.get_public_page;

import java.util.List;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.domain.SocialLinks;

/**
 * What the public page of a node shows. Only people known to be adults are named. A group or
 * ministry shows its leaders and members; a church or location its ministerial positions and, when
 * the node allows it, the org chart. {@code members}, {@code positions} and {@code orgChart} are
 * empty or null when they do not apply. Address and phone are only given for churches and
 * locations.
 */
public record GetPublicPageResult(
    String name,
    Organization.Type type,
    String description,
    String schedule,
    String address,
    String phoneNumber,
    SocialLinks socialLinks,
    List<String> leaders,
    List<String> members,
    List<PositionPeople> positions,
    OrgChartNode orgChart) {

  public record PositionPeople(String position, List<String> people) {}

  /** {@code slug} links to the node's own page when it is public, and is null otherwise. */
  public record OrgChartNode(
      String name,
      Organization.Type type,
      String slug,
      List<String> leaders,
      List<OrgChartNode> children) {}
}
