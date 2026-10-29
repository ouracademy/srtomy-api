package org.ouracademy.ovejafiel.shared;

import java.util.NoSuchElementException;
import org.ouracademy.ovejafiel.shared.domain.ConflictException;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

  /**
   * Thrown by the iam module when the user lacks a permission. Mapped here because the catch-all
   * below would otherwise turn it into a 500 before Spring Security sees it.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleForbidden(AccessDeniedException ex, WebRequest request) {
    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setTitle("Forbidden");
    return handleExceptionInternal(
        ex, problemDetail, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
  }

  /** A broken domain rule; its message is meant for the end user. */
  @ExceptionHandler(DomainValidationException.class)
  public ResponseEntity<Object> handleDomainValidation(
      DomainValidationException ex, WebRequest request) {
    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Bad Request");
    return handleExceptionInternal(
        ex, problemDetail, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  /** The request needs a confirmation or a change first; {@code code} says which case it is. */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
    var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Conflict");
    problemDetail.setProperty("code", ex.getCode());
    return handleExceptionInternal(
        ex, problemDetail, new HttpHeaders(), HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
    // The client only gets a generic 500, so the cause must at least reach the logs.
    logger.error("Unexpected error handling " + request.getDescription(false), ex);
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
