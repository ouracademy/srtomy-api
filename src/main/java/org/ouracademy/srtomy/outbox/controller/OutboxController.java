package org.ouracademy.srtomy.outbox.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.outbox.command.drain_outbox.DrainOutboxCommand;
import org.ouracademy.srtomy.outbox.command.drain_outbox.DrainOutboxCommandHandler;
import org.ouracademy.srtomy.outbox.command.drain_outbox.DrainOutboxResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Cloud Scheduler (never by the web app) every minute to attempt the messages that are
 * due. Only the scheduler's service account may call it: see {@code InternalSecurityConfig}.
 */
@RestController
@AllArgsConstructor
@Hidden
public class OutboxController {

  private static final int LIMIT = 50;

  DrainOutboxCommandHandler drainOutboxCommandHandler;

  @PostMapping("internal/outbox/drain")
  public DrainOutboxResult drain() {
    return drainOutboxCommandHandler.handle(new DrainOutboxCommand(LIMIT));
  }
}
