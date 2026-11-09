package org.ouracademy.ovejafiel.whatsapp.command.request_setup_link;

/**
 * Makes the link the pastor opens to connect the church's WhatsApp. A new link replaces the
 * previous one at the provider, and the connection goes back to pending.
 */
public interface RequestSetupLinkCommandHandler {
  RequestSetupLinkResult handle(RequestSetupLinkCommand command);
}
