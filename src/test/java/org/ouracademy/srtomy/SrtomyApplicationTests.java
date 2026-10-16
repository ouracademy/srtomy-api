package org.ouracademy.srtomy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// Without TestSecurityConfig, SecurityConfig's real JwtDecoder bean would call out to Auth0's
// live OIDC discovery endpoint on every context load — never acceptable in a test.
@SpringBootTest
@Import(TestSecurityConfig.class)
class SrtomyApplicationTests {

  @Test
  void contextLoads() {}
}
