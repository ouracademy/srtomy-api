package org.ouracademy.srtomy.notification.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

class EmailOutboxHandlerTests {

  EmailGateway gateway = mock(EmailGateway.class);
  EmailOutboxHandler handler = new EmailOutboxHandler(gateway);

  @Test
  void handlesTheEmailType() {
    assertThat(handler.type()).isEqualTo("EMAIL");
  }

  @Test
  void sendsThePayloadUsingTheMessageIdAsIdempotencyKey() {
    var id = OutboxMessageId.generate();

    handler.handle(id, "{\"to\":\"ana@x.org\",\"subject\":\"Hola\",\"text\":\"Texto\"}");

    verify(gateway).send(id.value().toString(), "ana@x.org", "Hola", "Texto");
  }

  @Test
  void anUnreadablePayloadFails() {
    assertThatThrownBy(() -> handler.handle(OutboxMessageId.generate(), "not json"))
        .isInstanceOf(IllegalStateException.class);
  }
}
