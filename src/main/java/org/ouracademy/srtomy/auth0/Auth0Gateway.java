package org.ouracademy.srtomy.auth0;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class Auth0Gateway {

  private static final String RESILIENCE_INSTANCE = "auth0";

  Auth0Config auth0Config;
  RestClient restClient;

  public Auth0Gateway(Auth0Config auth0Config) {
    this.auth0Config = auth0Config;
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(5));
    restClient =
        RestClient.builder().baseUrl(auth0Config.url).requestFactory(requestFactory).build();
  }

  @Retry(name = RESILIENCE_INSTANCE)
  @CircuitBreaker(name = RESILIENCE_INSTANCE)
  public AccessToken getAccessToken() {
    return restClient
        .post()
        .uri("/oauth/token")
        .contentType(MediaType.APPLICATION_JSON)
        .body(auth0Config)
        .retrieve()
        .body(AccessToken.class);
  }

  @Retry(name = RESILIENCE_INSTANCE)
  @CircuitBreaker(name = RESILIENCE_INSTANCE)
  public ResponseEntity<String> updateAppMetadata(
      AccessToken accessToken, String userId, Map<String, UUID> userMetadata) {
    return restClient
        .patch()
        .uri(uriBuilder -> uriBuilder.path("/api/v2/users/{userId}").build(userId))
        .headers(
            headers -> {
              headers.setBearerAuth(accessToken.getAccessToken());
            })
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("app_metadata", userMetadata))
        .retrieve()
        .toEntity(String.class);
  }
}
