package org.ouracademy.ovejafiel.shared;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * <p>This class lives in {@code shared}, not in the base package, so the defaults that Spring Boot
 * derives from the main class's package are widened explicitly to the whole base package {@code
 * org.ouracademy.ovejafiel}:
 *
 * <ul>
 *   <li>{@code scanBasePackages}: component scan ({@code @Controller}, {@code @Service},
 *       {@code @Configuration}, ...) of every module, not only {@code shared..}.
 *   <li>{@code @AutoConfigurationPackage}: the package Spring Data JPA uses to find {@code @Entity}
 *       classes and repository interfaces. Without it only {@code shared..} would be scanned and no
 *       module entity/repository would be registered. Unlike {@code @EntityScan}/{@code
 *       EnableJpaRepositories}, it is a no-op in slices that do not load JPA (e.g. {@code
 *       WebMvcTest}).
 * </ul>
 */
@SpringBootApplication(scanBasePackages = OvejafielApplication.BASE_PACKAGE)
@AutoConfigurationPackage(basePackages = OvejafielApplication.BASE_PACKAGE)
public class OvejafielApplication {

  static final String BASE_PACKAGE = "org.ouracademy.ovejafiel";

  public static void main(String[] args) {
    SpringApplication.run(OvejafielApplication.class, args);
  }
}
