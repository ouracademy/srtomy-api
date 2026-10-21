package org.ouracademy.srtomy.auth.gateway;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth0")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Auth0Config {
  String clientId;
  String clientSecret;

  String url = "https://srtommy.us.auth0.com";
  String audience = url + "/api/v2/";
  String grantType = "client_credentials";
}
