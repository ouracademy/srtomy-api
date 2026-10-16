package org.ouracademy.srtomy;

import java.util.NoSuchElementException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns RFC 7807 problem+json for unhandled exceptions instead of leaking Spring's default error
 * body (which can include a stack trace). Built on Spring's own {@link ProblemDetail} support (no
 * third-party library needed) — {@link ResponseEntityExceptionHandler} already covers the standard
 * MVC exceptions (validation, missing params, etc.); this adds the fallback for everything else,
 * plus the common "not found" shape from a repository's {@code .orElseThrow()}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Object> handleNotFound(NoSuchElementException ex, WebRequest request) {
    var problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problemDetail.setTitle("Not Found");
    return handleExceptionInternal(
        ex, problemDetail, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
    var problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setTitle("Internal Server Error");
    return handleExceptionInternal(
        ex, problemDetail, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (body == null) {
      var problemDetail = ProblemDetail.forStatus(statusCode);
      problemDetail.setTitle(statusCode.toString());
      body = problemDetail;
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }
}
