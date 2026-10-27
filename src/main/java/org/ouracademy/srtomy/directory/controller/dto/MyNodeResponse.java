package org.ouracademy.srtomy.directory.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.directory.query.get_my_nodes.MyNodeResult;
import org.ouracademy.srtomy.organization.domain.Organization;

/** A group, ministry or location where the user's own person is a member or a leader. */
public record MyNodeResponse(
    UUID id,
    String name,
    Organization.Type type,
    UUID churchId,
    String churchName,
    boolean leader,
    List<String> positions) {

  public static MyNodeResponse from(MyNodeResult result) {
    return new MyNodeResponse(
        result.id().value(),
        result.name(),
        result.type(),
        result.churchId().value(),
        result.churchName(),
        result.leader(),
        result.positions());
  }
}
