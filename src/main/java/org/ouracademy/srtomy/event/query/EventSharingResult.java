package org.ouracademy.srtomy.event.query;

import java.util.List;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventShare;
import org.ouracademy.srtomy.event.domain.EventShareId;
import org.ouracademy.srtomy.event.domain.GeneralAccess;
import org.ouracademy.srtomy.event.domain.LinkRole;
import org.ouracademy.srtomy.event.domain.ShareRole;

/** How an event is shared: its general access and link, the people it is shared with. */
public record EventSharingResult(
    GeneralAccess generalAccess,
    LinkRole linkRole,
    String slug,
    boolean isACourse,
    List<Share> shares,
    long registrations) {

  public record Share(EventShareId id, String email, ShareRole role) {

    public static Share from(EventShare share) {
      return new Share(share.getId(), share.getEmail(), share.getRole());
    }
  }

  public static EventSharingResult of(Event event, List<EventShare> shares, long registrations) {
    return new EventSharingResult(
        event.getGeneralAccess(),
        event.getLinkRole(),
        event.getShareSlug(),
        Boolean.TRUE.equals(event.getIsACourse()),
        shares.stream().map(Share::from).toList(),
        registrations);
  }
}
