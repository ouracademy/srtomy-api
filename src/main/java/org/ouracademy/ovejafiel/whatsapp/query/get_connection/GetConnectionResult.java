package org.ouracademy.ovejafiel.whatsapp.query.get_connection;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;

public record GetConnectionResult(
    State state,
    ConnectionMode mode,
    String displayNumber,
    LocalDateTime connectedAt,
    /** True when asked about a branch: the number is the church's. */
    boolean inherited,
    PartyId churchId,
    /** Why it is disconnected; null otherwise. */
    DisconnectReason disconnectReason,
    /** The pastor paused the assistant (the number is still connected). */
    boolean agentPaused) {

  public enum State {
    NOT_CONNECTED,
    PENDING,
    CONNECTED,
    DISCONNECTED
  }
}
