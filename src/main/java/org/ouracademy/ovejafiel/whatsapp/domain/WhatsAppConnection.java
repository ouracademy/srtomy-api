package org.ouracademy.ovejafiel.whatsapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/** The WhatsApp number of a church, connected through the provider. */
@Entity
@Getter
public class WhatsAppConnection extends DomainObject {

  @Id PartyId organizationId;

  @Column(nullable = false, unique = true)
  String kapsoCustomerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  ConnectionMode mode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  ConnectionStatus status = ConnectionStatus.PENDING;

  String phoneNumberId;
  String displayNumber;
  LocalDateTime connectedAt;

  protected WhatsAppConnection() {
    // jpa only
  }

  public WhatsAppConnection(PartyId organizationId, String kapsoCustomerId, ConnectionMode mode) {
    this.organizationId = organizationId;
    this.kapsoCustomerId = kapsoCustomerId;
    this.mode = mode;
  }

  /** A new setup link: the pastor starts again, possibly in another mode. */
  public void startSetup(ConnectionMode mode) {
    this.mode = mode;
    this.status = ConnectionStatus.PENDING;
  }

  public void markConnected(String phoneNumberId, String displayNumber, LocalDateTime now) {
    this.phoneNumberId = phoneNumberId;
    this.displayNumber = displayNumber;
    this.status = ConnectionStatus.CONNECTED;
    this.connectedAt = now;
  }

  public void disconnect() {
    this.status = ConnectionStatus.DISCONNECTED;
  }

  public boolean isConnected() {
    return status == ConnectionStatus.CONNECTED;
  }
}
