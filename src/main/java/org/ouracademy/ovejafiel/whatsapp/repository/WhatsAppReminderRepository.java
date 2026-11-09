package org.ouracademy.ovejafiel.whatsapp.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The event reminders already queued, one per registration. */
@Repository
public class WhatsAppReminderRepository {

  private final JdbcClient jdbc;

  WhatsAppReminderRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * @return true the first time a registration is marked, false if it already was
   */
  public boolean markQueued(UUID registrationId, LocalDateTime now) {
    return jdbc.sql(
                "insert into whatsapp_reminder(registration_id, queued_at) values (:id, :now)"
                    + " on conflict do nothing")
            .param("id", registrationId)
            .param("now", now)
            .update()
        == 1;
  }
}
