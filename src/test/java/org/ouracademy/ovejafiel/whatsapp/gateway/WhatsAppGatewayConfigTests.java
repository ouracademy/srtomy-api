package org.ouracademy.ovejafiel.whatsapp.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WhatsAppGatewayConfigTests {

  private final WhatsAppGatewayConfig config = new WhatsAppGatewayConfig();

  @Test
  void withoutAKeyNothingLeavesTheLog() {
    assertThat(config.whatsAppGateway("", "https://kapso.test", "customer_managed"))
        .isInstanceOf(LoggingWhatsAppGateway.class);
  }

  @Test
  void withAKeyTheGatewayTalksToKapsoInEitherBillingMode() {
    assertThat(config.whatsAppGateway("key", "https://kapso.test", "customer_managed"))
        .isInstanceOf(KapsoWhatsAppGateway.class);
    assertThat(config.whatsAppGateway("key", "https://kapso.test", "partner_managed"))
        .isInstanceOf(KapsoWhatsAppGateway.class);
  }

  @Test
  void aMistypedBillingModeStopsTheStartupInsteadOfBreakingEverySetupLink() {
    assertThatThrownBy(() -> config.whatsAppGateway("key", "https://kapso.test", "client_managed"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("kapso.billing-mode");
  }
}
