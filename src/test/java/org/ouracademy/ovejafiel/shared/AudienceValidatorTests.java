package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTests {

  private final AudienceValidator validator = new AudienceValidator("https://api.srtommy.com");

  @Test
  void succeedsWhenTheRequiredAudienceIsPresent() {
    var jwt = mock(Jwt.class);
    when(jwt.getAudience()).thenReturn(List.of("https://api.srtommy.com"));

    assertThat(validator.validate(jwt).hasErrors()).isFalse();
  }

  @Test
  void failsWhenTheAudienceIsForADifferentApi() {
    var jwt = mock(Jwt.class);
    when(jwt.getAudience()).thenReturn(List.of("https://some-other-api.example.com"));

    assertThat(validator.validate(jwt).hasErrors()).isTrue();
  }

  @Test
  void failsWhenThereIsNoAudienceAtAll() {
    var jwt = mock(Jwt.class);
    when(jwt.getAudience()).thenReturn(List.of());

    assertThat(validator.validate(jwt).hasErrors()).isTrue();
  }
}
