package org.ouracademy.ovejafiel.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.notification.SentEmails;
import org.ouracademy.ovejafiel.notification.TestEmailConfig;
import org.ouracademy.ovejafiel.shared.ChurchApiTests;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** The daily renewal reminder, as Cloud Scheduler calls it, with the paid period set by hand. */
@Import(TestEmailConfig.class)
class RenewalRemindersIntegrationTests extends ChurchApiTests {

  @Autowired JdbcTemplate jdbc;
  @Autowired SentEmails sentEmails;

  JsonNode remind(String token) {
    var result =
        client
            .post()
            .uri("/internal/billing/remind")
            .headers(h -> h.setBearerAuth(token))
            .exchange()
            .expectBody(String.class)
            .returnResult();
    assertThat(result.getStatus().value()).isEqualTo(200);
    try {
      return JSON.readTree(result.getResponseBody());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void paidUntil(LocalDate day) {
    jdbc.update(
        "insert into billing_subscription (organization_id, country, paid_until) values (?, 'PE', ?)"
            + " on conflict (organization_id) do update set paid_until = excluded.paid_until",
        UUID.fromString(church),
        day);
  }

  int reminders() {
    return jdbc.queryForObject(
        "select count(*) from outbox_message where type = 'BILLING_RENEWAL' and recipient = ?",
        Integer.class,
        ownerEmail());
  }

  String ownerEmail() {
    return owner.replace("auth0|", "") + "@example.org";
  }

  @Test
  void theOwnerIsRemindedAWeekBeforeAndOnlyOnce() {
    paidUntil(LocalDate.now().plusDays(7));

    remind(TestSecurityConfig.schedulerToken(jwtEncoder));
    remind(TestSecurityConfig.schedulerToken(jwtEncoder));

    assertThat(reminders()).isEqualTo(1);
    var email =
        sentEmails.all().stream().filter(e -> e.to().equals(ownerEmail())).toList().getLast();
    assertThat(email.subject()).isEqualTo("El plan Iglesia de Iglesia vence en 7 días");
    assertThat(email.text()).contains("/portal/" + church + "/billing");
  }

  @Test
  void aChurchIsRemindedTheDayBeforeAndWhenGraceStarts() {
    paidUntil(LocalDate.now().plusDays(1));
    remind(TestSecurityConfig.schedulerToken(jwtEncoder));
    assertThat(reminders()).isEqualTo(1);

    paidUntil(LocalDate.now().minusDays(1));
    remind(TestSecurityConfig.schedulerToken(jwtEncoder));
    assertThat(reminders()).isEqualTo(2);
  }

  @Test
  void aChurchFarFromItsEndIsNotReminded() {
    paidUntil(LocalDate.now().plusDays(20));
    remind(TestSecurityConfig.schedulerToken(jwtEncoder));
    assertThat(reminders()).isZero();
  }

  @Test
  void onlyTheSchedulerMayRunIt() {
    var status =
        client
            .post()
            .uri("/internal/billing/remind")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.agentToken(jwtEncoder)))
            .exchange()
            .returnResult(String.class)
            .getStatus()
            .value();
    assertThat(status).isEqualTo(403);
  }
}
