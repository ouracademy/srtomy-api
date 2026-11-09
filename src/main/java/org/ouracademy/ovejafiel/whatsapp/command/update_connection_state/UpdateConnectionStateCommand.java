package org.ouracademy.ovejafiel.whatsapp.command.update_connection_state;

import java.util.List;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;

/**
 * The provider says some numbers stopped working, or work again.
 *
 * @param reason why they stopped; null when they work again
 */
public record UpdateConnectionStateCommand(
    String idempotencyKey, List<String> phoneNumberIds, DisconnectReason reason) {}
