package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/**
 * The rate per IP of the public endpoints, with a limit of two a minute. Each test calls from an IP
 * of its own, because the buckets stay in the database between runs.
 */
@TestPropertySource(properties = {"rate-limit.enabled=true", "rate-limit.public.per-minute=2"})
class PublicRateLimitIntegrationTests extends ChurchApiTests {

  static String randomIp() {
    var bytes = UUID.randomUUID().getMostSignificantBits();
    return "10." + (bytes & 0xff) + "." + ((bytes >> 8) & 0xff) + "." + ((bytes >> 16) & 0xff);
  }

  /** Marking attendance with a QR code that does not exist: refused, but it counts. */
  EntityExchangeResult<String> checkIn(String forwardedFor) {
    return client
        .post()
        .uri("/public/attendance/not-a-real-code")
        .header("X-Forwarded-For", forwardedFor)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{}")
        .exchange()
        .expectBody(String.class)
        .returnResult();
  }

  @Test
  void anIpThatCallsTooOftenWaits() {
    var ip = randomIp();
    assertThat(checkIn(ip).getStatus().value()).isNotEqualTo(429);
    assertThat(checkIn(ip).getStatus().value()).isNotEqualTo(429);

    var third = checkIn(ip);

    assertThat(third.getStatus().value()).isEqualTo(429);
    assertThat(Integer.parseInt(third.getResponseHeaders().getFirst("Retry-After")))
        .isBetween(1, 60);
    assertThat(third.getResponseBody()).contains("rate.limit").contains("Espera");
    // Another IP is not affected.
    assertThat(checkIn(randomIp()).getStatus().value()).isNotEqualTo(429);
  }

  @Test
  void theCallerCannotPickItsIpWithTheFirstForwardedAddress() {
    var ip = randomIp();
    checkIn("1.1.1.1, " + ip);
    checkIn("2.2.2.2, " + ip);

    assertThat(checkIn("3.3.3.3, " + ip).getStatus().value()).isEqualTo(429);
  }

  @Test
  void signedInEndpointsAreNotLimited() {
    for (var i = 0; i < 4; i++) {
      get("/organizations/" + church + "/billing");
    }
  }

  @Test
  void theIpIsTheLastForwardedAddressOrTheRemoteOne() {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("169.254.1.1");
    assertThat(PublicRateLimitFilter.clientIp(request)).isEqualTo("169.254.1.1");
    request.addHeader("X-Forwarded-For", "8.8.8.8, 190.12.1.4");
    assertThat(PublicRateLimitFilter.clientIp(request)).isEqualTo("190.12.1.4");
  }
}
