package org.ouracademy.srtomy.notification.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.PermanentDeliveryException;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

class EmailOutboxHandlerTests {

  EmailGateway gateway = mock(EmailGateway.class);
  EmailOutboxHandler handler = new EmailOutboxHandler(gateway);

  @Test
  void handlesTheEmailChannel() {
    assertThat(handler.channel()).isEqualTo(OutboxChannel.EMAIL);
  }

  @Test
  void sendsTheContentUsingTheMessageIdAsIdempotencyKey() {
    var id = OutboxMessageId.generate();
    when(gateway.send(id.value().toString(), "ana@x.org", "Hola", "Texto")).thenReturn("re_1");

    var providerId = handler.deliver(id, "ana@x.org", "{\"subject\":\"Hola\",\"text\":\"Texto\"}");

    assertThat(providerId).isEqualTo("re_1");
    verify(gateway).send(id.value().toString(), "ana@x.org", "Hola", "Texto");
  }

  @Test
  void unreadableContentIsGivenUpAtOnce() {
    assertThatThrownBy(() -> handler.deliver(OutboxMessageId.generate(), "ana@x.org", "not json"))
        .isInstanceOf(PermanentDeliveryException.class);
  }
}
