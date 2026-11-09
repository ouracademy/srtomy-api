package org.ouracademy.ovejafiel.whatsapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/** The WhatsApp number of a church, connected through the provider. */
@Entity
@Table(name = "whatsapp_connection")
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

  /** The account the message templates of the church belong to; null while unknown. */
  String businessAccountId;

  LocalDateTime connectedAt;

  /** Why it is not connected any more; null while connected. */
  @Enumerated(EnumType.STRING)
  DisconnectReason disconnectReason;

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
    markConnected(phoneNumberId, displayNumber, null, now);
  }

  public void markConnected(
      String phoneNumberId, String displayNumber, String businessAccountId, LocalDateTime now) {
    this.phoneNumberId = phoneNumberId;
    this.displayNumber = displayNumber;
    this.businessAccountId = businessAccountId;
    this.status = ConnectionStatus.CONNECTED;
    this.connectedAt = now;
    this.disconnectReason = null;
  }

  /** The pastor disconnects it from the portal. */
  public void disconnect() {
    lose(DisconnectReason.PASTOR);
  }

  /** The provider says the number does not work any more. */
  public void lose(DisconnectReason reason) {
    this.status = ConnectionStatus.DISCONNECTED;
    this.disconnectReason = reason;
  }

  /**
   * The provider says the number works again. It is ignored unless the number was lost for a reason
   * that can heal: a number the pastor disconnected stays disconnected.
   *
   * @return whether the connection is connected again
   */
  public boolean restore() {
    if (status != ConnectionStatus.DISCONNECTED
        || disconnectReason == null
        || !disconnectReason.healsOnItsOwn()) {
      return false;
    }
    this.status = ConnectionStatus.CONNECTED;
    this.disconnectReason = null;
    return true;
  }

  public boolean isConnected() {
    return status == ConnectionStatus.CONNECTED;
  }
}
