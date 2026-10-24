package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionHolderResult;

/** {@code email} is the linked user's, null when the person has no account. */
public record PositionHolderResponse(
    UUID personId, String name, String organizationName, String email) {

  public static PositionHolderResponse from(PositionHolderResult result) {
    return new PositionHolderResponse(
        result.personId().value(), result.name(), result.organizationName(), result.email());
  }
}
