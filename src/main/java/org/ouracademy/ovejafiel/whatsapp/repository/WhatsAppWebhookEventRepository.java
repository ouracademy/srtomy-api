package org.ouracademy.ovejafiel.whatsapp.repository;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The webhooks already handled, by the idempotency key of the provider. */
@Repository
public class WhatsAppWebhookEventRepository {

  private final JdbcClient jdbc;

  WhatsAppWebhookEventRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * @return true the first time a key is seen, false for a repeat
   */
  public boolean markReceived(String idempotencyKey, LocalDateTime now) {
    return jdbc.sql(
                "insert into whatsapp_webhook_event(idempotency_key, received_at)"
                    + " values (:key, :now) on conflict do nothing")
            .param("key", idempotencyKey)
            .param("now", now)
            .update()
        == 1;
  }
}
