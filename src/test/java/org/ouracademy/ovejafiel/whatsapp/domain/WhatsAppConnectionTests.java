package org.ouracademy.ovejafiel.whatsapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class WhatsAppConnectionTests {

  private WhatsAppConnection connected() {
    var connection =
        new WhatsAppConnection(PartyId.generate(), "cus-1", ConnectionMode.COEXISTENCE);
    connection.markConnected("pn-1", "+51 999 111 222", LocalDateTime.now());
    return connection;
  }

  @Test
  void aNumberTheProviderLostKeepsTheReasonAndStopsBeingConnected() {
    var connection = connected();

    connection.lose(DisconnectReason.ACCESS_REMOVED);

    assertThat(connection.isConnected()).isFalse();
    assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
    assertThat(connection.getDisconnectReason()).isEqualTo(DisconnectReason.ACCESS_REMOVED);
  }

  @Test
  void aLostNumberWorksAgainWhenTheProviderSaysSo() {
    var connection = connected();
    connection.lose(DisconnectReason.ACCOUNT_DISABLED);

    assertThat(connection.restore()).isTrue();

    assertThat(connection.isConnected()).isTrue();
    assertThat(connection.getDisconnectReason()).isNull();
  }

  @Test
  void aNumberThePastorDisconnectedIsNotBroughtBackByTheProvider() {
    var connection = connected();
    connection.disconnect();

    assertThat(connection.getDisconnectReason()).isEqualTo(DisconnectReason.PASTOR);
    assertThat(connection.restore()).isFalse();
    assertThat(connection.isConnected()).isFalse();
  }

  @Test
  void restoringAConnectedOrPendingNumberChangesNothing() {
    var connected = connected();
    assertThat(connected.restore()).isFalse();
    assertThat(connected.isConnected()).isTrue();

    var pending = new WhatsAppConnection(PartyId.generate(), "cus-2", ConnectionMode.DEDICATED);
    assertThat(pending.restore()).isFalse();
    assertThat(pending.getStatus()).isEqualTo(ConnectionStatus.PENDING);
  }

  @Test
  void connectingAgainClearsTheReason() {
    var connection = connected();
    connection.lose(DisconnectReason.NUMBER_DELETED);

    connection.markConnected("pn-2", "+51 999 333 444", LocalDateTime.now());

    assertThat(connection.getDisconnectReason()).isNull();
  }

  @Test
  void pausingTheAssistantKeepsTheNumberConnectedAndAReconnectionStartsItOn() {
    var connection = connected();

    connection.pauseAgent();

    assertThat(connection.isAgentPaused()).isTrue();
    assertThat(connection.isConnected()).isTrue();

    connection.markConnected("pn-2", "+51 999 333 444", LocalDateTime.now());

    assertThat(connection.isAgentPaused()).isFalse();
  }
}
