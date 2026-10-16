package org.ouracademy.srtomy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
}
