package org.ouracademy.ovejafiel.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The key that signs the tokens of the WhatsApp agent; with none set there are no such tokens. */
@Configuration
public class DelegationConfig {

  @Bean
  @ConditionalOnExpression("!'${delegation.private-key:}'.isEmpty()")
  DelegatedTokens delegatedTokens(@Value("${delegation.private-key}") String privateKey) {
    return DelegatedTokens.fromPem(privateKey);
  }
}
