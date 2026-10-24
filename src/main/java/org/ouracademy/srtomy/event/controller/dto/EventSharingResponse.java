package org.ouracademy.srtomy.event.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.event.domain.GeneralAccess;
import org.ouracademy.srtomy.event.domain.LinkRole;
import org.ouracademy.srtomy.event.domain.ShareRole;
import org.ouracademy.srtomy.event.query.EventSharingResult;

/** How an event is shared; the link of the shared page is {@code /e/{slug}}. */
public record EventSharingResponse(
    GeneralAccess generalAccess,
    LinkRole linkRole,
    String slug,
    boolean isACourse,
    List<ShareResponse> shares,
    long registrations) {

  public record ShareResponse(UUID id, String email, ShareRole role) {

    static ShareResponse from(EventSharingResult.Share share) {
      return new ShareResponse(share.id().value(), share.email(), share.role());
    }
  }

  public static ShareResponse shareFrom(EventSharingResult.Share share) {
    return ShareResponse.from(share);
  }

  public static EventSharingResponse from(EventSharingResult result) {
    return new EventSharingResponse(
        result.generalAccess(),
        result.linkRole(),
        result.slug(),
        result.isACourse(),
        result.shares().stream().map(ShareResponse::from).toList(),
        result.registrations());
  }
}
