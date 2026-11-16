package org.ouracademy.ovejafiel.shared;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Limits how often one IP may call the endpoints anyone can call without signing in and that write
 * something (signing up to an event, cancelling it, marking attendance with the QR, the complaints
 * book), so nobody can flood them. The buckets live in Postgres (Bucket4j), so the limit holds with
 * any number of instances. Over the limit the answer is 429 with {@code Retry-After}.
 *
 * <p>The IP is the last address of {@code X-Forwarded-For}: the one Google's front end added in
 * Cloud Run, which the caller cannot choose (the ones before it can be anything).
 */
@Slf4j
public class PublicRateLimitFilter extends OncePerRequestFilter {

  private static final PathPatternParser PARSER = new PathPatternParser();

  /** What is limited: method and path. */
  record Limited(HttpMethod method, PathPattern path) {
    boolean matches(HttpServletRequest request) {
      return method.matches(request.getMethod())
          && path.matches(org.springframework.http.server.PathContainer.parsePath(uri(request)));
    }

    private static String uri(HttpServletRequest request) {
      var context = request.getContextPath();
      var uri = request.getRequestURI();
      return context.isEmpty() ? uri : uri.substring(context.length());
    }
  }

  static final List<Limited> LIMITED =
      List.of(
          new Limited(HttpMethod.POST, PARSER.parse("/v1/public/events/*/registrations")),
          new Limited(HttpMethod.DELETE, PARSER.parse("/v1/public/registrations/*")),
          new Limited(HttpMethod.POST, PARSER.parse("/v1/public/attendance/*")),
          new Limited(HttpMethod.POST, PARSER.parse("/v1/complaints")));

  private final ProxyManager<String> buckets;
  private final BucketConfiguration configuration;

  public PublicRateLimitFilter(ProxyManager<String> buckets, int perMinute) {
    this.buckets = buckets;
    this.configuration =
        BucketConfiguration.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(perMinute)
                    .refillGreedy(perMinute, Duration.ofMinutes(1))
                    .build())
            .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return LIMITED.stream().noneMatch(limited -> limited.matches(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var key = "public:" + clientIp(request);
    io.github.bucket4j.ConsumptionProbe probe;
    try {
      probe = buckets.getProxy(key, () -> configuration).tryConsumeAndReturnRemaining(1);
    } catch (RuntimeException e) {
      // The database decides the request anyway; a failing limiter must not block it.
      log.warn("Rate limiter unavailable; letting the request through", e);
      chain.doFilter(request, response);
      return;
    }
    if (probe.isConsumed()) {
      chain.doFilter(request, response);
      return;
    }
    var seconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader("Retry-After", String.valueOf(seconds));
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"code\":\"rate.limit\",\"detail\":\"Demasiados intentos seguidos. Espera "
                + seconds
                + " segundos e inténtalo de nuevo.\"}");
  }

  static String clientIp(HttpServletRequest request) {
    var forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      var parts = forwarded.split(",");
      var last = parts[parts.length - 1].trim();
      if (!last.isEmpty()) {
        return last;
      }
    }
    return request.getRemoteAddr();
  }
}
