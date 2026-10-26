package org.ouracademy.ovejafiel.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kept separate from {@link OvejafielApplication} on purpose: {@code @WebMvcTest} always loads the
 * nearest {@code @SpringBootConfiguration} class (here, {@code OvejafielApplication}) to discover
 * web-layer beans, but its type filter excludes plain {@code @Configuration} classes like this one.
 * If {@code @EnableJpaAuditing} stayed directly on {@code OvejafielApplication}, every
 * {@code @WebMvcTest} slice would try to wire the JPA auditing machinery with no {@code
 * EntityManagerFactory} present and fail to start.
 */
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {}
