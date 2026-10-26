package org.ouracademy.ovejafiel.notification.command.send_email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.notification.gateway.EmailGateway;

@ExtendWith(MockitoExtension.class)
class SendEmailCommandHandlerImplTests {

  @Mock EmailGateway emailGateway;
  @InjectMocks SendEmailCommandHandlerImpl handler;

  @Test
  void handsTheMessageToTheGateway() {
    var result = handler.handle(new SendEmailCommand("ana@x.org", "Hola", "Texto"));

    assertThat(result.delivered()).isTrue();
    verify(emailGateway).send("ana@x.org", "Hola", "Texto");
  }

  @Test
  void aProviderFailureIsReportedNotThrown() {
    doThrow(new IllegalStateException("provider down"))
        .when(emailGateway)
        .send("a@x.org", "S", "T");

    var result = handler.handle(new SendEmailCommand("a@x.org", "S", "T"));

    assertThat(result.delivered()).isFalse();
  }
}
