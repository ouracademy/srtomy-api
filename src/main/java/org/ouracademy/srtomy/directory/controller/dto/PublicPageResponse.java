package org.ouracademy.srtomy.directory.controller.dto;

import java.util.List;
import org.ouracademy.srtomy.directory.query.get_public_page.GetPublicPageResult;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.domain.SocialLinks;

/** The public page of a node, with only adults named; see {@link GetPublicPageResult}. */
public record PublicPageResponse(
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

  /** {@code slug} links to the page of the node itself when it is public, and is null otherwise. */
  public record OrgChartNode(
      String name,
      Organization.Type type,
      String slug,
      List<String> leaders,
      List<OrgChartNode> children) {

    static OrgChartNode from(GetPublicPageResult.OrgChartNode node) {
      return new OrgChartNode(
          node.name(),
          node.type(),
          node.slug(),
          node.leaders(),
          node.children().stream().map(OrgChartNode::from).toList());
    }
  }

  public static PublicPageResponse from(GetPublicPageResult result) {
    return new PublicPageResponse(
        result.name(),
        result.type(),
        result.description(),
        result.schedule(),
        result.address(),
        result.phoneNumber(),
        result.socialLinks(),
        result.leaders(),
        result.members(),
        result.positions().stream()
            .map(entry -> new PositionPeople(entry.position(), entry.people()))
            .toList(),
        result.orgChart() == null ? null : OrgChartNode.from(result.orgChart()));
  }
}
