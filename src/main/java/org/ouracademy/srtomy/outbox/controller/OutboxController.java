package org.ouracademy.srtomy.outbox.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryCommand;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryCommandHandler;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryResult;
import org.ouracademy.srtomy.outbox.command.dispatch_pending.DispatchPendingCommand;
import org.ouracademy.srtomy.outbox.command.dispatch_pending.DispatchPendingCommandHandler;
import org.ouracademy.srtomy.outbox.command.dispatch_pending.DispatchPendingResult;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Cloud Tasks (one delivery attempt) and by Cloud Scheduler (the daily sweep), never by
 * the web app. Only the service account of the scheduler may call them: see {@code
 * InternalSecurityConfig}.
 */
@RestController
@AllArgsConstructor
@Hidden
public class OutboxController {

  /** Messages nothing touched for this long have no task on the way. */
  private static final Duration UNTOUCHED_FOR = Duration.ofMinutes(15);

  private static final int SWEEP_LIMIT = 500;

  AttemptDeliveryCommandHandler attemptDeliveryCommandHandler;
  DispatchPendingCommandHandler dispatchPendingCommandHandler;

  /** 503 asks Cloud Tasks to call again later; any other answer ends the task. */
  @PostMapping("internal/outbox/{id}/attempt")
  public ResponseEntity<AttemptDeliveryResult> attempt(@PathVariable UUID id) {
    var result =
        attemptDeliveryCommandHandler.handle(new AttemptDeliveryCommand(new OutboxMessageId(id)));
    return ResponseEntity.status(
            result.shouldRetry() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK)
        .body(result);
  }

  @PostMapping("internal/outbox/dispatch-pending")
  public DispatchPendingResult dispatchPending() {
    return dispatchPendingCommandHandler.handle(
        new DispatchPendingCommand(UNTOUCHED_FOR, SWEEP_LIMIT));
  }
}
