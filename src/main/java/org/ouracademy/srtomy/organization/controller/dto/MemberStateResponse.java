package org.ouracademy.srtomy.organization.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.organization.query.MemberStateResult;

/**
 * Keeps the nesting of the Accountability JSON this endpoint used to return ({@code child.name},
 * {@code type.name}, ...), restricted to the fields the organization module can expose.
 */
public record MemberStateResponse(UUID id, PartyRef child, PartyRef parent, TypeRef type) {

  public record PartyRef(UUID id, String name) {}

  public record TypeRef(UUID id, String name) {}

  public static MemberStateResponse from(MemberStateResult result) {
    return new MemberStateResponse(
        result.id().value(),
        new PartyRef(result.childId().value(), result.childName()),
        new PartyRef(result.parentId().value(), null),
        new TypeRef(result.stateId().value(), result.stateName()));
  }
}
