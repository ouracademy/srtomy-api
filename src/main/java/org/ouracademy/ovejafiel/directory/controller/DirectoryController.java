package org.ouracademy.ovejafiel.directory.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.directory.controller.dto.DirectoryEntryResponse;
import org.ouracademy.ovejafiel.directory.controller.dto.DirectoryNodeResponse;
import org.ouracademy.ovejafiel.directory.controller.dto.MyNodeResponse;
import org.ouracademy.ovejafiel.directory.controller.dto.PublicPageResponse;
import org.ouracademy.ovejafiel.directory.query.get_church_directory.GetChurchDirectoryQuery;
import org.ouracademy.ovejafiel.directory.query.get_church_directory.GetChurchDirectoryQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_directory_entry.GetDirectoryEntryQuery;
import org.ouracademy.ovejafiel.directory.query.get_directory_entry.GetDirectoryEntryQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_my_nodes.GetMyNodesQuery;
import org.ouracademy.ovejafiel.directory.query.get_my_nodes.GetMyNodesQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageQuery;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The church directory (for anyone with access in the church) and the public page of a node (for
 * anyone with the link). The visibility of each node decides who sees its people.
 */
@RestController
@AllArgsConstructor
@Tag(name = "directory")
public class DirectoryController {

  GetChurchDirectoryQueryHandler getChurchDirectoryQueryHandler;
  GetDirectoryEntryQueryHandler getDirectoryEntryQueryHandler;
  GetPublicPageQueryHandler getPublicPageQueryHandler;
  GetMyNodesQueryHandler getMyNodesQueryHandler;

  /** Every node of the church with its basic information. */
  @GetMapping("organizations/{churchId}/directory")
  public List<DirectoryNodeResponse> directory(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID churchId) {
    return getChurchDirectoryQueryHandler
        .handle(new GetChurchDirectoryQuery(jwt.getSubject(), new PartyId(churchId)))
        .nodes()
        .stream()
        .map(DirectoryNodeResponse::from)
        .toList();
  }

  /** The groups and ministries where the signed-in user's own person is a member or a leader. */
  @GetMapping("me/nodes")
  public List<MyNodeResponse> myNodes(@AuthenticationPrincipal Jwt jwt) {
    return getMyNodesQueryHandler.handle(new GetMyNodesQuery(jwt.getSubject())).stream()
        .map(MyNodeResponse::from)
        .toList();
  }

  /** A node of the directory, with its people when its visibility lets the caller see them. */
  @GetMapping("organizations/{nodeId}/directory-entry")
  public DirectoryEntryResponse entry(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID nodeId) {
    return DirectoryEntryResponse.from(
        getDirectoryEntryQueryHandler.handle(
            new GetDirectoryEntryQuery(jwt.getSubject(), new PartyId(nodeId))));
  }

  /** The public page of a node; 404 unless the node is public. No sign-in needed. */
  @GetMapping("public/organizations/{slug}")
  public PublicPageResponse publicPage(@PathVariable String slug) {
    return getPublicPageQueryHandler
        .handle(new GetPublicPageQuery(slug))
        .map(PublicPageResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Page not found"));
  }
}
