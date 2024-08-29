package org.ouracademy.srtomy.auth0;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class Auth0Gateway {

    Auth0Config auth0Config;

    AccessToken getAccessToken() {
        RestClient restClient = RestClient.builder()
                .baseUrl(auth0Config.url)
                .build();

        return restClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(auth0Config)
                .retrieve()
                .body(AccessToken.class);
    }

}
