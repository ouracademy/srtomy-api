package org.ouracademy.ovejafiel.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.ChurchApiTests;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;

/**
 * The four plans from end to end, with the sandbox gateway (every payment counts as paid). The free
 * plan holds two people and the Basic one three, so their limits are quick to reach; the Church
 * plan holds two locations.
 */
@TestPropertySource(
    properties = {
      "billing.free.max-people=2",
      "billing.free.custom-roles=false",
      "billing.basic.max-people=3",
      "billing.church.max-locations=2",
    })
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

  static JsonNode planNamed(JsonNode billing, String plan) {
    for (var candidate : billing.get("plans")) {
      if (candidate.get("plan").asText().equals(plan)) {
        return candidate;
      }
    }
    throw new AssertionError("No plan " + plan);
  }

  static JsonNode priceIn(JsonNode plan, String country) {
    for (var price : plan.get("prices")) {
      if (price.get("country").asText().equals(country)) {
        return price;
      }
    }
    throw new AssertionError("No price in " + country);
  }

  String checkoutBody(String plan, String country, int months, boolean ownNumber) {
    return "{\"plan\":\"%s\",\"country\":\"%s\",\"months\":%d,\"ownNumber\":%s}"
        .formatted(plan, country, months, ownNumber);
  }

  JsonNode pay(String plan, String country, int months, boolean ownNumber) {
    post(billing() + "/checkout", checkoutBody(plan, country, months, ownNumber));
    return post(billing() + "/sync", null);
  }

  @Test
  void aNewChurchIsOnTheFreePlanWithItsLimitsAndSeesTheFourPlans() {
    var plan = get(billing());

    assertThat(plan.get("plan").asText()).isEqualTo("FREE");
    assertThat(plan.get("status").asText()).isEqualTo("FREE");
    assertThat(plan.get("people").asInt()).isEqualTo(1);
    assertThat(plan.get("locations").asInt()).isEqualTo(1);
    assertThat(plan.get("maxPeople").asInt()).isEqualTo(2);
    assertThat(plan.get("maxLocations").asInt()).isEqualTo(1);
    assertThat(plan.get("paidUntil").isNull()).isTrue();
    assertThat(plan.get("ownNumber").asBoolean()).isFalse();
    for (var feature : plan.get("features")) {
      assertThat(feature.get("enabled").asBoolean()).isFalse();
    }
    assertThat(plan.get("ai").get("limit").asInt()).isEqualTo(20);
    assertThat(plan.get("reminders").get("limit").asInt()).isZero();

    assertThat(plan.get("plans")).hasSize(4);
    var free = planNamed(plan, "FREE");
    assertThat(free.get("label").asText()).isEqualTo("Gratis");
    assertThat(free.get("prices")).isEmpty();
    var basic = planNamed(plan, "BASIC");
    assertThat(basic.get("label").asText()).isEqualTo("Básico");
    assertThat(basic.get("maxPeople").asInt()).isEqualTo(3);
    assertThat(basic.get("aiMessages").asInt()).isEqualTo(150);
    assertThat(basic.get("whatsappReminders").asInt()).isEqualTo(50);
    assertThat(basic.get("ownNumberExtra").asBoolean()).isTrue();
    assertThat(basic.get("fits").asBoolean()).isTrue();
    assertThat(priceIn(basic, "PE").get("monthly").asInt()).isEqualTo(45);
    assertThat(priceIn(basic, "PE").get("yearly").asInt()).isEqualTo(450);
    assertThat(priceIn(basic, "PE").get("ownNumberMonthly").asInt()).isEqualTo(49);
    assertThat(priceIn(basic, "CO").get("monthly").asInt()).isEqualTo(49000);
    var church = planNamed(plan, "CHURCH");
    assertThat(priceIn(church, "PE").get("monthly").asInt()).isEqualTo(75);
    assertThat(priceIn(church, "CO").get("monthly").asInt()).isEqualTo(79000);
    var network = planNamed(plan, "NETWORK");
    assertThat(network.get("maxPeople").isNull()).isTrue();
    assertThat(network.get("ownNumberIncluded").asBoolean()).isTrue();
    assertThat(priceIn(network, "PE").get("monthly").asInt()).isEqualTo(149);
    assertThat(priceIn(network, "CO").get("yearly").asInt()).isEqualTo(1590000);
  }

  @Test
  void theFreePlanRefusesOneMorePersonOrLocationAndSuggestsTheNextPlan() {
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
    assertThat(person.body().get("detail").asText())
        .contains("plan Gratis incluye hasta 2 personas")
        .contains("plan Básico");

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
  void theFreePlanCannotCreateRolesOfItsOwnButBasicCan() {
    var role = "{\"name\":\"Tesorero\",\"description\":\"d\",\"permissions\":[\"org.read\"]}";
    var free = call(HttpMethod.POST, "/organizations/" + church + "/roles", role, owner);
    assertThat(free.status()).isEqualTo(402);
    assertThat(free.body().get("code").asText()).isEqualTo("feature.custom_roles");

    pay("BASIC", "PE", 1, false);

    assertThat(call(HttpMethod.POST, "/organizations/" + church + "/roles", role, owner).status())
        .isIn(200, 201);
  }

  @Test
  void payingBasicRaisesTheLimitToItsOwnAndStopsThere() {
    createPerson("Ana", location);
    checkout("BASIC");
    // Until the payment is confirmed nothing changes.
    assertThat(get(billing()).get("plan").asText()).isEqualTo("FREE");
    var pending = get(billing() + "/invoices");
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).get("status").asText()).isEqualTo("PENDING");
    assertThat(pending.get(0).get("plan").asText()).isEqualTo("BASIC");

    var plan = post(billing() + "/sync", null);

    var paidUntil = LocalDate.now().plusMonths(1).minusDays(1);
    assertThat(plan.get("plan").asText()).isEqualTo("BASIC");
    assertThat(plan.get("status").asText()).isEqualTo("ACTIVE");
    assertThat(plan.get("paidUntil").asText()).isEqualTo(paidUntil.toString());
    assertThat(plan.get("country").asText()).isEqualTo("PE");
    assertThat(plan.get("maxPeople").asInt()).isEqualTo(3);
    assertThat(plan.get("ai").get("limit").asInt()).isEqualTo(150);
    assertThat(plan.get("reminders").get("limit").asInt()).isEqualTo(50);
    assertThat(enabled(plan, "EXCEL_EXPORT")).isTrue();
    assertThat(enabled(plan, "CUSTOM_ROLES")).isTrue();
    assertThat(enabled(plan, "FINANCES")).isFalse();
    assertThat(enabled(plan, "OWN_NUMBER")).isFalse();
    var invoice = get(billing() + "/invoices").get(0);
    assertThat(invoice.get("status").asText()).isEqualTo("PAID");
    assertThat(invoice.get("periodEnd").asText()).isEqualTo(paidUntil.toString());
    assertThat(invoice.get("checkoutUrl").isNull()).isTrue();

    // What the free plan refused now works, up to the three people of Basic (the owner is one).
    createPerson("Beto", location);
    var fourth =
        call(
            HttpMethod.POST,
            "/people",
            "{\"firstName\":\"Dora\",\"lastName\":\"Diaz\",\"organizationId\":\"%s\"}"
                .formatted(location),
            owner);
    assertThat(fourth.status()).isEqualTo(402);
    assertThat(fourth.body().get("detail").asText())
        .contains("plan Básico incluye hasta 3 personas")
        .contains("plan Iglesia");
    // A second location needs the Church plan.
    assertThat(
            call(
                    HttpMethod.POST,
                    "/organizations/locations",
                    "{\"name\":\"Sede Norte\",\"parentId\":\"%s\"}".formatted(church),
                    owner)
                .status())
        .isEqualTo(402);

    // Checking again credits nothing more.
    assertThat(post(billing() + "/sync", null).get("paidUntil").asText())
        .isEqualTo(paidUntil.toString());
  }

  @Test
  void theChurchPlanHasFiveTimesTheLocationsOfBasicAndTheFeaturesOfTheChurch() {
    var plan = pay("CHURCH", "PE", 1, false);

    assertThat(plan.get("plan").asText()).isEqualTo("CHURCH");
    assertThat(plan.get("maxLocations").asInt()).isEqualTo(2);
    assertThat(plan.get("ai").get("limit").asInt()).isEqualTo(400);
    assertThat(plan.get("reminders").get("limit").asInt()).isEqualTo(200);
    assertThat(enabled(plan, "FINANCES")).isTrue();
    assertThat(enabled(plan, "MASS_EMAIL")).isTrue();
    assertThat(enabled(plan, "VOLUNTEERS")).isTrue();
    assertThat(enabled(plan, "OWN_NUMBER")).isFalse();
    post(
        "/organizations/locations",
        "{\"name\":\"Sede Norte\",\"parentId\":\"%s\"}".formatted(church));
    assertThat(
            call(
                    HttpMethod.POST,
                    "/organizations/locations",
                    "{\"name\":\"Sede Sur\",\"parentId\":\"%s\"}".formatted(church),
                    owner)
                .status())
        .isEqualTo(402);
  }

  @Test
  void networkHasNoMaximumAndIncludesTheOwnNumber() {
    var plan = pay("NETWORK", "PE", 1, false);

    assertThat(plan.get("plan").asText()).isEqualTo("NETWORK");
    assertThat(plan.get("maxPeople").isNull()).isTrue();
    assertThat(plan.get("maxLocations").isNull()).isTrue();
    assertThat(plan.get("ai").get("limit").asInt()).isEqualTo(1200);
    assertThat(plan.get("reminders").get("limit").asInt()).isEqualTo(600);
    assertThat(plan.get("ownNumber").asBoolean()).isTrue();
    assertThat(enabled(plan, "OWN_NUMBER")).isTrue();
    createPerson("Ana", location);
    createPerson("Beto", location);
    createPerson("Carla", location);
  }

  @Test
  void theOwnNumberIsRefusedWithoutThePlanOrTheExtraAndAllowedWithThem() {
    var setup = "/organizations/" + church + "/whatsapp/setup-link";
    var body = "{\"mode\":\"COEXISTENCE\"}";
    var free = call(HttpMethod.POST, setup, body, owner);
    assertThat(free.status()).isEqualTo(402);
    assertThat(free.body().get("code").asText()).isEqualTo("feature.own_number");
    assertThat(free.body().get("detail").asText()).contains("número de Sr. Tommy");

    pay("CHURCH", "PE", 1, false);
    assertThat(call(HttpMethod.POST, setup, body, owner).status()).isEqualTo(402);

    var withExtra = pay("CHURCH", "PE", 1, true);
    assertThat(withExtra.get("ownNumber").asBoolean()).isTrue();
    assertThat(call(HttpMethod.POST, setup, body, owner).status()).isIn(200, 201);
  }

  @Test
  void theExtraNumberCostsItsOwnPriceEveryMonthAndNetworkRefusesIt() {
    var month = post(billing() + "/checkout", checkoutBody("BASIC", "PE", 1, true));
    assertThat(month.get("amount").asInt()).isEqualTo(94);
    var year = post(billing() + "/checkout", checkoutBody("CHURCH", "CO", 12, true));
    assertThat(year.get("amount").asInt()).isEqualTo(1280000);
    assertThat(year.get("currency").asText()).isEqualTo("COP");

    var network =
        call(
            HttpMethod.POST,
            billing() + "/checkout",
            checkoutBody("NETWORK", "PE", 1, true),
            owner);
    assertThat(network.status()).isEqualTo(400);
    assertThat(network.body().get("detail").asText()).contains("ya incluye");
  }

  @Test
  void aPlanTheChurchAlreadyOutgrewCannotBeBought() {
    pay("CHURCH", "PE", 1, false);
    createPerson("Ana", location);
    createPerson("Beto", location);
    createPerson("Carla", location);

    var basic =
        call(
            HttpMethod.POST, billing() + "/checkout", checkoutBody("BASIC", "PE", 1, false), owner);

    // Basic holds three people in this test, and the church has four with its owner.
    assertThat(basic.status()).isEqualTo(400);
    assertThat(basic.body().get("detail").asText()).contains("Elige un plan más grande");
    assertThat(get(billing() + "/invoices")).hasSize(1);
    assertThat(planNamed(get(billing()), "BASIC").get("fits").asBoolean()).isFalse();
    assertThat(planNamed(get(billing()), "CHURCH").get("fits").asBoolean()).isTrue();
    checkout("CHURCH");
  }

  @Test
  void renewingAYearFromColombiaWithAnotherPlanAddsTwelveMonthsAfterWhatIsPaid() {
    pay("BASIC", "PE", 1, false);

    var checkout = post(billing() + "/checkout", checkoutBody("CHURCH", "CO", 12, false));
    assertThat(checkout.get("currency").asText()).isEqualTo("COP");
    assertThat(checkout.get("amount").asInt()).isEqualTo(790000);
    var plan = post(billing() + "/sync", null);

    assertThat(plan.get("paidUntil").asText())
        .isEqualTo(LocalDate.now().plusMonths(1).plusMonths(12).minusDays(1).toString());
    assertThat(plan.get("country").asText()).isEqualTo("CO");
    assertThat(plan.get("plan").asText()).isEqualTo("CHURCH");
    assertThat(get(billing() + "/invoices")).hasSize(2);
  }

  @Test
  void onlyAMonthOrAYearAKnownCountryAndAPaidPlanCanBePaid() {
    for (var body :
        new String[] {
          checkoutBody("CHURCH", "PE", 3, false),
          checkoutBody("CHURCH", "AR", 1, false),
          checkoutBody("FREE", "PE", 1, false),
          "{\"country\":\"PE\",\"months\":1}",
        }) {
      assertThat(call(HttpMethod.POST, billing() + "/checkout", body, owner).status())
          .as(body)
          .isEqualTo(400);
    }
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
                    checkoutBody("CHURCH", "PE", 1, false),
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

  JsonNode checkout(String plan) {
    return post(billing() + "/checkout", checkoutBody(plan, "PE", 1, false));
  }
}
