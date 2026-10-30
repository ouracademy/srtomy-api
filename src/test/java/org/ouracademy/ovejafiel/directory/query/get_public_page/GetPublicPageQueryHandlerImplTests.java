package org.ouracademy.ovejafiel.directory.query.get_public_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader;
import org.ouracademy.ovejafiel.directory.query.DirectoryReader.Audience;
import org.ouracademy.ovejafiel.directory.repository.DirectoryRepository;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.domain.SocialLinks;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetPublicPageQueryHandlerImplTests {

  DirectoryRepository repository = mock(DirectoryRepository.class);
  DirectoryReader reader = mock(DirectoryReader.class);
  FindOrganizationSubtreeQueryHandler subtree = mock(FindOrganizationSubtreeQueryHandler.class);
  GetPublicPageQueryHandlerImpl handler =
      new GetPublicPageQueryHandlerImpl(repository, reader, subtree);

  Organization church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
  Organization open =
      organization(PartyId.generate(), "Abierta", Organization.Type.LOCATION, church);
  Organization hidden = organization(PartyId.generate(), "Oculta", Organization.Type.GROUP, open);
  Organization nested =
      organization(PartyId.generate(), "Dentro de la oculta", Organization.Type.GROUP, hidden);
  Organization churchOnly =
      organization(PartyId.generate(), "Solo iglesia", Organization.Type.GROUP, open);

  @BeforeEach
  void setUp() {
    church.setAddress("Av. 1");
    church.setPhoneNumber("999");
    church.setSchedule("Domingos");
    church.setSocialLinks(new SocialLinks("https://x.org", null, null, null, null));
    hidden.setMemberVisibility(MemberVisibility.LEADERS);
    churchOnly.setMemberVisibility(MemberVisibility.CHURCH);
    when(repository.findBySlug("church")).thenReturn(Optional.of(church));
    var all = List.of(church, open, hidden, nested, churchOnly);
    when(subtree.handle(any()))
        .thenReturn(
            new FindOrganizationSubtreeResult(all.stream().map(Organization::getId).toList()));
    when(reader.nodes(any())).thenReturn(all);
    when(reader.teamByNode(any(), any(Audience.class)))
        .thenReturn(Map.of(open.getId(), List.of("Ana Ruiz")));
    var positions = new LinkedHashMap<String, List<String>>();
    positions.put("Pastor", List.of("Luis Paz"));
    when(reader.positionsOf(church.getId(), Audience.PUBLIC)).thenReturn(positions);
  }

  @Test
  void aNodeThatIsNotPublicHasNoPage() {
    church.setMemberVisibility(MemberVisibility.CHURCH);
    assertThat(handler.handle(new GetPublicPageQuery("church"))).isEmpty();
    church.setMemberVisibility(MemberVisibility.MEMBERS);
    assertThat(handler.handle(new GetPublicPageQuery("church"))).isEmpty();
    church.setMemberVisibility(MemberVisibility.LEADERS);
    assertThat(handler.handle(new GetPublicPageQuery("church"))).isEmpty();
  }

  @Test
  void anUnknownSlugHasNoPage() {
    when(repository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetPublicPageQuery("nope"))).isEmpty();
  }

  @Test
  void aChurchPageShowsContactPositionsAndTheOrgChartWithoutTheLeaderOnlyNodes() {
    var page = handler.handle(new GetPublicPageQuery("church")).orElseThrow();

    assertThat(page.name()).isEqualTo("Iglesia");
    assertThat(page.address()).isEqualTo("Av. 1");
    assertThat(page.phoneNumber()).isEqualTo("999");
    assertThat(page.schedule()).isEqualTo("Domingos");
    assertThat(page.socialLinks().website()).isEqualTo("https://x.org");
    assertThat(page.members()).isNull();
    assertThat(page.positions()).extracting(p -> p.position()).containsExactly("Pastor");

    var chart = page.orgChart();
    assertThat(chart.name()).isEqualTo("Iglesia");
    assertThat(chart.children()).extracting(c -> c.name()).containsExactly("Abierta");
    var location = chart.children().get(0);
    assertThat(location.team()).containsExactly("Ana Ruiz");
    // The leader-only group and everything below it are left out.
    assertThat(location.children()).extracting(c -> c.name()).containsExactly("Solo iglesia");
  }

  @Test
  void theOrgChartLinksOnlyToNodesWithAPublicPage() {
    var chart = handler.handle(new GetPublicPageQuery("church")).orElseThrow().orgChart();

    assertThat(chart.slug()).isEqualTo(church.getSlug());
    var location = chart.children().get(0);
    assertThat(location.slug()).isEqualTo(open.getSlug());
    assertThat(location.children().get(0).slug()).isNull();
  }

  @Test
  void theOrgChartCanBeTurnedOff() {
    church.setShowOrgChart(false);

    assertThat(handler.handle(new GetPublicPageQuery("church")).orElseThrow().orgChart()).isNull();
  }

  @Test
  void aGroupPageShowsLeadersAndMembersButNoContactOrChart() {
    var group = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP, open);
    group.setAddress("Privado");
    group.setPhoneNumber("111");
    group.setSchedule("Viernes");
    when(repository.findBySlug("group")).thenReturn(Optional.of(group));
    when(reader.teamByNode(List.of(group.getId()), Audience.PUBLIC))
        .thenReturn(Map.of(group.getId(), List.of("Ana Ruiz")));
    when(reader.membersOf(group.getId(), Audience.PUBLIC)).thenReturn(List.of("Beto Paz"));

    var page = handler.handle(new GetPublicPageQuery("group")).orElseThrow();

    assertThat(page.team()).containsExactly("Ana Ruiz");
    assertThat(page.members()).containsExactly("Beto Paz");
    assertThat(page.schedule()).isEqualTo("Viernes");
    assertThat(page.address()).isNull();
    assertThat(page.phoneNumber()).isNull();
    assertThat(page.orgChart()).isNull();
    assertThat(page.positions()).isEmpty();
  }
}
