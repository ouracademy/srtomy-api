package org.ouracademy.ovejafiel.directory.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.directory.query.get_my_nodes.MyNodeResult;
import org.ouracademy.ovejafiel.organization.domain.Organization;

/** A group, ministry or location where the user's own person is on the list or holds a position. */
public record MyNodeResponse(
    UUID id,
    String name,
    Organization.Type type,
    UUID churchId,
    String churchName,
    List<String> positions) {

  public static MyNodeResponse from(MyNodeResult result) {
    return new MyNodeResponse(
        result.id().value(),
        result.name(),
        result.type(),
        result.churchId().value(),
        result.churchName(),
        result.positions());
  }
}
