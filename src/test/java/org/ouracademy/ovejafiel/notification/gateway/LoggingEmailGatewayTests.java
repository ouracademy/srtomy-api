package org.ouracademy.ovejafiel.notification.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingEmailGatewayTests {

  @Test
  void sendingOnlyLogs() {
    assertThatCode(() -> new LoggingEmailGateway().send("msg-1", "a@x.org", "S", "T"))
        .doesNotThrowAnyException();
  }
}
