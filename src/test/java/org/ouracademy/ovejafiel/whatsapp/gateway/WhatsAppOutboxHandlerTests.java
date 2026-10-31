package org.ouracademy.ovejafiel.whatsapp.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;

class WhatsAppOutboxHandlerTests {

  private final UUID churchId = UUID.randomUUID();
  private final WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  private final WhatsAppGateway gateway = mock(WhatsAppGateway.class);
  private final WhatsAppOutboxHandler handler = new WhatsAppOutboxHandler(connections, gateway);

  private String payload(String template, String variables) {
    return "{\"organizationId\":\""
        + churchId
        + "\",\"template\":\""
        + template
        + "\",\"variables\":"
        + variables
        + "}";
  }

  private void connected() {
    var connection =
        new WhatsAppConnection(new PartyId(churchId), "cus-1", ConnectionMode.DEDICATED);
    connection.markConnected("pn-1", "+51 999 111 222", LocalDateTime.now());
    when(connections.findById(new PartyId(churchId))).thenReturn(Optional.of(connection));
  }

  @Test
  void handlesTheWhatsappChannel() {
    assertThat(handler.channel()).isEqualTo(OutboxChannel.WHATSAPP);
  }

  @Test
  void sendsTheTemplateFromTheChurchNumberToTheDigitsOfTheRecipient() {
    connected();
    when(gateway.sendTemplate(any(), any(), any(), any(), any())).thenReturn("wamid.1");

    var id =
        handler.deliver(
            OutboxMessageId.generate(),
            "+51 999-888-777",
            payload("WELCOME", "[\"Ana\",\"Iglesia Central\"]"));

    assertThat(id).isEqualTo("wamid.1");
    verify(gateway)
        .sendTemplate("pn-1", "51999888777", "welcome", "es", List.of("Ana", "Iglesia Central"));
  }

  @Test
  void givesUpWhenTheChurchHasNoConnection() {
    when(connections.findById(new PartyId(churchId))).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.deliver(
                    OutboxMessageId.generate(), "51999", payload("WELCOME", "[\"Ana\",\"X\"]")))
        .isInstanceOf(PermanentDeliveryException.class);
    verifyNoInteractions(gateway);
  }

  @Test
  void givesUpWhenTheConnectionWasCutOff() {
    var connection =
        new WhatsAppConnection(new PartyId(churchId), "cus-1", ConnectionMode.DEDICATED);
    connection.markConnected("pn-1", "+51", LocalDateTime.now());
    connection.disconnect();
    when(connections.findById(new PartyId(churchId))).thenReturn(Optional.of(connection));

    assertThatThrownBy(
            () ->
                handler.deliver(
                    OutboxMessageId.generate(), "51999", payload("WELCOME", "[\"Ana\",\"X\"]")))
        .isInstanceOf(PermanentDeliveryException.class);
    verifyNoInteractions(gateway);
  }

  @Test
  void givesUpOnWrongVariables() {
    connected();

    assertThatThrownBy(
            () ->
                handler.deliver(
                    OutboxMessageId.generate(), "51999", payload("EVENT_REMINDER", "[\"Ana\"]")))
        .isInstanceOf(PermanentDeliveryException.class);
    verifyNoInteractions(gateway);
  }

  @Test
  void givesUpOnUnreadablePayloads() {
    assertThatThrownBy(() -> handler.deliver(OutboxMessageId.generate(), "51999", "not json"))
        .isInstanceOf(PermanentDeliveryException.class);
    assertThatThrownBy(
            () ->
                handler.deliver(
                    OutboxMessageId.generate(), "51999", payload("NO_SUCH_TEMPLATE", "[]")))
        .isInstanceOf(PermanentDeliveryException.class);
  }

  @Test
  void aTransientFailureOfTheProviderIsNotPermanent() {
    connected();
    when(gateway.sendTemplate(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("timeout"));

    assertThatThrownBy(
            () ->
                handler.deliver(
                    OutboxMessageId.generate(), "51999", payload("WELCOME", "[\"Ana\",\"X\"]")))
        .isInstanceOf(IllegalStateException.class)
        .isNotInstanceOf(PermanentDeliveryException.class);
  }
}
