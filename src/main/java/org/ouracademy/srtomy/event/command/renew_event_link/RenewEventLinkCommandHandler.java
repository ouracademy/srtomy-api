package org.ouracademy.srtomy.event.command.renew_event_link;

/** Revokes the link of the event: the old address stops working and a new one is made. */
public interface RenewEventLinkCommandHandler {
  RenewEventLinkResult handle(RenewEventLinkCommand command);
}
