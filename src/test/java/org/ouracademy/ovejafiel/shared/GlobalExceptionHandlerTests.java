package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

/**
 * Plain unit test for the handler methods themselves (status + body shape). The content negotiation
 * that actually produces an "application/problem+json" response only happens during real MVC
 * dispatch, not when calling the method directly — that part is covered end-to-end by
 * EventControllerTest#deletingAMissingEventReturnsA404Problem.
 */
class GlobalExceptionHandlerTests {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final WebRequest request = mock(WebRequest.class);

  @Test
  void noSuchElementExceptionBecomesA404Problem() {
    var response = handler.handleNotFound(new NoSuchElementException("nope"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    var body = (ProblemDetail) response.getBody();
    assertThat(body.getStatus()).isEqualTo(404);
  }

  @Test
  void unexpectedExceptionBecomesA500ProblemWithoutLeakingTheExceptionMessage() {
    var response = handler.handleUnexpected(new IllegalStateException("boom"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    var body = (ProblemDetail) response.getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isNull();
  }

  @Test
  void accessDeniedBecomesA403ProblemWithItsMessage() {
    var response = handler.handleForbidden(new AccessDeniedException("sin permiso"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    var body = (ProblemDetail) response.getBody();
    assertThat(body.getStatus()).isEqualTo(403);
    assertThat(body.getDetail()).isEqualTo("sin permiso");
  }

  @Test
  void domainValidationBecomesA400ProblemWithItsMessage() {
    var response =
        handler.handleDomainValidation(new DomainValidationException("regla rota"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var body = (ProblemDetail) response.getBody();
    assertThat(body.getDetail()).isEqualTo("regla rota");
  }
}
