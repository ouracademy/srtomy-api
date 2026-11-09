package org.ouracademy.ovejafiel.whatsapp.command.disconnect;

/**
 * Stops using the church's WhatsApp. Messages still waiting fail with a clear reason when their
 * turn comes, because the church has no connection any more.
 */
public interface DisconnectCommandHandler {
  DisconnectResult handle(DisconnectCommand command);
}
