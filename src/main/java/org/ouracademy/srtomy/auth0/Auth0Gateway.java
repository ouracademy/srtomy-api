package org.ouracademy.srtomy.auth0;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class Auth0Gateway {
    Auth0Config auth0Config;
    RestClient restClient;

    public Auth0Gateway(Auth0Config auth0Config) {
        this.auth0Config = auth0Config;
        restClient = RestClient.builder()
                .baseUrl(auth0Config.url)
                .build();
    }

    public AccessToken getAccessToken() {
        return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(auth0Config)
                .retrieve()
                .body(AccessToken.class);
    }

    public ResponseEntity<String> updateAppMetadata(AccessToken accessToken, String userId,
            Map<String, Long> userMetadata) {
        return restClient.patch()
                .uri("/api/v2/users/" + userId)
                .headers(headers -> {
                    headers.setBearerAuth(accessToken.getAccessToken());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("app_metadata", userMetadata))
                .retrieve()
                .toEntity(String.class);
    }

}
