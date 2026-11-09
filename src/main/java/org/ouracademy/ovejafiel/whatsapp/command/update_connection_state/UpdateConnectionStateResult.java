package org.ouracademy.ovejafiel.whatsapp.command.update_connection_state;

/**
 * @param changed the connections that changed state
 * @param alreadyHandled the same delivery arrived again
 */
public record UpdateConnectionStateResult(int changed, boolean alreadyHandled) {}
