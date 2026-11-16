package org.ouracademy.ovejafiel.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.ChurchApiTests;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

/**
 * The plans from end to end, with the sandbox gateway (every payment counts as paid) and a free
 * plan of two people so its limit is quick to reach.
 */
@TestPropertySource(properties = "billing.free.max-people=2")
class BillingIntegrationTests extends ChurchApiTests {

  String billing() {
    return "/organizations/" + church + "/billing";
  }

  static boolean enabled(JsonNode billing, String feature) {
    for (var state : billing.get("features")) {
      if (state.get("feature").asText().equals(feature)) {
        return state.get("enabled").asBoolean();
      }
    }
    throw new AssertionError("No feature " + feature);
  }

  @Test
  void aNewChurchIsOnTheFreePlanWithItsLimits() {
    var plan = get(billing());

    assertThat(plan.get("plan").asText()).isEqualTo("FREE");
    assertThat(plan.get("status").asText()).isEqualTo("FREE");
    assertThat(plan.get("people").asInt()).isEqualTo(1);
    assertThat(plan.get("locations").asInt()).isEqualTo(1);
    assertThat(plan.get("maxPeople").asInt()).isEqualTo(2);
    assertThat(plan.get("maxLocations").asInt()).isEqualTo(1);
    assertThat(plan.get("paidUntil").isNull()).isTrue();
    // WhatsApp can be tried; the rest is of the paid plan.
    assertThat(enabled(plan, "WHATSAPP")).isTrue();
    assertThat(enabled(plan, "FINANCES")).isFalse();
    assertThat(enabled(plan, "MASS_EMAIL")).isFalse();
    assertThat(plan.get("whatsappTrialEndsAt").isNull()).isTrue();
    assertThat(plan.get("whatsappTrialDays").asInt()).isEqualTo(15);
    var peru = plan.get("prices").get(0);
    assertThat(peru.get("country").asText()).isEqualTo("PE");
    assertThat(peru.get("currency").asText()).isEqualTo("PEN");
    assertThat(peru.get("monthly").asInt()).isEqualTo(75);
    assertThat(peru.get("yearly").asInt()).isEqualTo(750);
  }

  @Test
  void theFreePlanRefusesOneMorePersonOrLocationThanItIncludes() {
    createPerson("Ana", location);

    var person =
        call(
            HttpMethod.POST,
            "/people",
            "{\"firstName\":\"Beto\",\"lastName\":\"Diaz\",\"organizationId\":\"%s\"}"
                .formatted(location),
            owner);
    assertThat(person.status()).isEqualTo(402);
    assertThat(person.body().get("code").asText()).isEqualTo("limit.people");
    assertThat(person.body().get("detail").asText()).contains("2 personas");

    var second =
        call(
            HttpMethod.POST,
            "/organizations/locations",
            "{\"name\":\"Sede Norte\",\"parentId\":\"%s\"}".formatted(church),
            owner);
    assertThat(second.status()).isEqualTo(402);
    assertThat(second.body().get("code").asText()).isEqualTo("limit.locations");

    assertThat(get(billing()).get("people").asInt()).isEqualTo(2);
  }

  @Test
  void payingAMonthGivesThePaidPlanWithoutLimitsAndIsNotCreditedTwice() {
    createPerson("Ana", location);

    var checkout = post(billing() + "/checkout", "{\"country\":\"PE\",\"months\":1}");
    assertThat(checkout.get("amount").asInt()).isEqualTo(75);
    assertThat(checkout.get("currency").asText()).isEqualTo("PEN");
    assertThat(checkout.get("checkoutUrl").asText())
        .endsWith("/portal/" + church + "/billing?pago=completado");
    // Until the payment is confirmed nothing changes.
    assertThat(get(billing()).get("plan").asText()).isEqualTo("FREE");
    var pending = get(billing() + "/invoices");
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).get("status").asText()).isEqualTo("PENDING");

    var plan = post(billing() + "/sync", null);

    var paidUntil = LocalDate.now().plusMonths(1).minusDays(1);
    assertThat(plan.get("plan").asText()).isEqualTo("CHURCH");
    assertThat(plan.get("status").asText()).isEqualTo("ACTIVE");
    assertThat(plan.get("paidUntil").asText()).isEqualTo(paidUntil.toString());
    assertThat(plan.get("country").asText()).isEqualTo("PE");
    assertThat(plan.get("maxPeople").isNull()).isTrue();
    assertThat(enabled(plan, "FINANCES")).isTrue();
    var invoice = get(billing() + "/invoices").get(0);
    assertThat(invoice.get("status").asText()).isEqualTo("PAID");
    assertThat(invoice.get("periodEnd").asText()).isEqualTo(paidUntil.toString());
    assertThat(invoice.get("checkoutUrl").isNull()).isTrue();

    // What the free plan refused now works.
    createPerson("Beto", location);
    post(
        "/organizations/locations",
        "{\"name\":\"Sede Norte\",\"parentId\":\"%s\"}".formatted(church));

    // Checking again credits nothing more.
    assertThat(post(billing() + "/sync", null).get("paidUntil").asText())
        .isEqualTo(paidUntil.toString());
  }

  @Test
  void renewingAYearFromColombiaAddsTwelveMonthsAfterWhatIsPaid() {
    post(billing() + "/checkout", "{\"country\":\"PE\",\"months\":1}");
    post(billing() + "/sync", null);

    var checkout = post(billing() + "/checkout", "{\"country\":\"CO\",\"months\":12}");
    assertThat(checkout.get("currency").asText()).isEqualTo("COP");
    assertThat(checkout.get("amount").asInt()).isEqualTo(850000);
    var plan = post(billing() + "/sync", null);

    assertThat(plan.get("paidUntil").asText())
        .isEqualTo(LocalDate.now().plusMonths(1).plusMonths(12).minusDays(1).toString());
    assertThat(plan.get("country").asText()).isEqualTo("CO");
    assertThat(get(billing() + "/invoices")).hasSize(2);
  }

  @Test
  void onlyAMonthOrAYearAndAKnownCountryCanBePaid() {
    assertThat(
            call(
                    HttpMethod.POST,
                    billing() + "/checkout",
                    "{\"country\":\"PE\",\"months\":3}",
                    owner)
                .status())
        .isEqualTo(400);
    assertThat(
            call(
                    HttpMethod.POST,
                    billing() + "/checkout",
                    "{\"country\":\"AR\",\"months\":1}",
                    owner)
                .status())
        .isEqualTo(400);
    assertThat(get(billing() + "/invoices")).isEmpty();
  }

  @Test
  void someoneWithoutAccessSeesNothingAndCannotPay() {
    var stranger = newUser("stranger");

    assertThat(call(HttpMethod.GET, billing(), null, stranger).status()).isEqualTo(403);
    assertThat(call(HttpMethod.GET, billing() + "/invoices", null, stranger).status())
        .isEqualTo(403);
    assertThat(
            call(
                    HttpMethod.POST,
                    billing() + "/checkout",
                    "{\"country\":\"PE\",\"months\":1}",
                    stranger)
                .status())
        .isEqualTo(403);
    assertThat(call(HttpMethod.POST, billing() + "/sync", null, stranger).status()).isEqualTo(403);
    assertThat(call(HttpMethod.GET, billing(), null, null).status()).isEqualTo(401);
  }

  @Test
  void theWebhookRefusesWhatTheGatewayDidNotSign() {
    var answer = call(HttpMethod.POST, "/webhooks/billing", "{\"payment_id\":\"SANDBOX-x\"}", null);

    assertThat(answer.status()).isEqualTo(401);
  }

  @Test
  void connectingWhatsappOnTheFreePlanStartsItsTrial() {
    post("/organizations/" + church + "/whatsapp/setup-link", "{\"mode\":\"COEXISTENCE\"}");

    var plan = get(billing());
    assertThat(plan.get("whatsappTrialEndsAt").isNull()).isFalse();
    assertThat(enabled(plan, "WHATSAPP")).isTrue();
  }
}
