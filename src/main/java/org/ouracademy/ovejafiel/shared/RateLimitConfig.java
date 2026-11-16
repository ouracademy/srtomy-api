package org.ouracademy.ovejafiel.shared;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * The rate per IP of the public endpoints ({@link PublicRateLimitFilter}), {@code
 * rate-limit.public.per-minute} requests a minute (30 by default). {@code rate-limit.enabled=false}
 * turns it off (the tests do, except the ones about it).
 */
@Configuration
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
class RateLimitConfig {

  @Bean
  FilterRegistrationBean<PublicRateLimitFilter> publicRateLimitFilter(
      DataSource dataSource, @Value("${rate-limit.public.per-minute:30}") int perMinute) {
    var buckets =
        Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
            .primaryKeyMapper(PrimaryKeyMapper.STRING)
            .table("rate_limit_bucket")
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                    Duration.ofMinutes(1)))
            .build();
    var registration = new FilterRegistrationBean<>(new PublicRateLimitFilter(buckets, perMinute));
    // Before Spring Security: a flood is refused without checking anything else.
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
