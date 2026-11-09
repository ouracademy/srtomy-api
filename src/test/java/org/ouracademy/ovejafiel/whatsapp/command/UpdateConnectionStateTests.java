package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommand;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;

/** What happens to our record of a number when the provider says it stopped or came back. */
class UpdateConnectionStateTests {

  private final WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  private final WhatsAppWebhookEventRepository webhookEvents =
      mock(WhatsAppWebhookEventRepository.class);

  private WhatsAppConnection connectedAs(String phoneNumberId) {
    var connection =
        new WhatsAppConnection(
            PartyId.generate(), "cus-" + phoneNumberId, ConnectionMode.COEXISTENCE);
    connection.markConnected(phoneNumberId, "+51 999 111 222", LocalDateTime.now());
    when(connections.findByPhoneNumberId(phoneNumberId)).thenReturn(Optional.of(connection));
    return connection;
  }

  private UpdateConnectionStateCommandHandler handler() {
    try {
      var type =
          Class.forName(
              "org.ouracademy.ovejafiel.whatsapp.command.update_connection_state."
                  + "UpdateConnectionStateCommandHandlerImpl");
      var constructor = type.getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      return (UpdateConnectionStateCommandHandler)
          constructor.newInstance(connections, webhookEvents);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void aLostNumberStopsBeingConnectedAndKeepsWhy() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    var connection = connectedAs("pn-1");

    var result =
        handler()
            .handle(
                new UpdateConnectionStateCommand(
                    "k1", List.of("pn-1"), DisconnectReason.ACCESS_REMOVED));

    assertThat(result.changed()).isEqualTo(1);
    assertThat(connection.isConnected()).isFalse();
    assertThat(connection.getDisconnectReason()).isEqualTo(DisconnectReason.ACCESS_REMOVED);
    verify(connections).save(connection);
  }

  @Test
  void aWholeAccountLosesEveryNumberItHasAndIgnoresNumbersThatAreNotOurs() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    var first = connectedAs("pn-1");
    var second = connectedAs("pn-2");
    when(connections.findByPhoneNumberId("pn-3")).thenReturn(Optional.empty());

    var result =
        handler()
            .handle(
                new UpdateConnectionStateCommand(
                    "k1", List.of("pn-1", "pn-2", "pn-3"), DisconnectReason.ACCOUNT_DISABLED));

    assertThat(result.changed()).isEqualTo(2);
    assertThat(first.isConnected()).isFalse();
    assertThat(second.isConnected()).isFalse();
  }

  @Test
  void aRestoredNumberIsConnectedAgain() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    var connection = connectedAs("pn-1");
    connection.lose(DisconnectReason.ACCOUNT_DISABLED);

    var result = handler().handle(new UpdateConnectionStateCommand("k1", List.of("pn-1"), null));

    assertThat(result.changed()).isEqualTo(1);
    assertThat(connection.isConnected()).isTrue();
  }

  @Test
  void aNumberThePastorDisconnectedDoesNotComeBackBecauseTheProviderSaysSo() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    var connection = connectedAs("pn-1");
    connection.disconnect();

    var result = handler().handle(new UpdateConnectionStateCommand("k1", List.of("pn-1"), null));

    assertThat(result.changed()).isZero();
    assertThat(connection.isConnected()).isFalse();
    verify(connections, never()).save(any());
  }

  @Test
  void theSameDeliveryTwiceChangesNothingTheSecondTime() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(false);
    var connection = connectedAs("pn-1");

    var result =
        handler()
            .handle(
                new UpdateConnectionStateCommand(
                    "k1", List.of("pn-1"), DisconnectReason.NUMBER_DELETED));

    assertThat(result.alreadyHandled()).isTrue();
    assertThat(connection.isConnected()).isTrue();
  }
}
