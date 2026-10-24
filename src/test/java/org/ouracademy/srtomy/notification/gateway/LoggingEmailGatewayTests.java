package org.ouracademy.srtomy.notification.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingEmailGatewayTests {

  @Test
  void sendingOnlyLogs() {
    assertThatCode(() -> new LoggingEmailGateway().send("a@x.org", "S", "T"))
        .doesNotThrowAnyException();
  }
}
