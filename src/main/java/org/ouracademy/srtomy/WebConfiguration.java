package org.ouracademy.srtomy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  @Value("${cors_origin}")
  private String corsOrigin;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedOrigins(corsOrigin).allowedMethods("*");
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    // Prefixes only @RestController-annotated classes, not a server-wide
    // context-path: actuator's health/liveness/readiness endpoints (registered
    // separately, not via @RestController) must stay unversioned so Cloud Run
    // and k8s probes don't need to know about API versioning at all. Also keeps
    // everything on the single port Cloud Run exposes, rather than needing a
    // separate management port.
    configurer.addPathPrefix("/v1", HandlerTypePredicate.forAnnotation(RestController.class));
  }
}
