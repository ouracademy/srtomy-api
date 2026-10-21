package org.ouracademy.srtomy.organization.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.organization.query.GetLocationChildrenResult;

/** A member or leader of a group/ministry; {@code kind} tells organizations and people apart. */
public record LocationChildMemberResponse(
    UUID id, String name, GetLocationChildrenResult.Kind kind) {

  public static LocationChildMemberResponse from(GetLocationChildrenResult.Child child) {
    return new LocationChildMemberResponse(child.id().value(), child.name(), child.kind());
  }
}
