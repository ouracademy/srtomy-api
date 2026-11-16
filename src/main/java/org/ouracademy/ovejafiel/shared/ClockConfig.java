package org.ouracademy.ovejafiel.shared;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The clock handlers ask for "now" and "today", so a test can fix the date. */
@Configuration
public class ClockConfig {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
