package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.whatsapp.command.provision_templates.ProvisionTemplatesCommand;
import org.ouracademy.ovejafiel.whatsapp.command.provision_templates.ProvisionTemplatesCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;

/** Submitting the message templates to Meta for the church's account. */
class ProvisionTemplatesTests {

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();

  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  WhatsAppGateway gateway = mock(WhatsAppGateway.class);

  ProvisionTemplatesCommandHandler handler() {
    try {
      var constructor =
          Class.forName(
                  "org.ouracademy.ovejafiel.whatsapp.command.provision_templates."
                      + "ProvisionTemplatesCommandHandlerImpl")
              .getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      return (ProvisionTemplatesCommandHandler)
          constructor.newInstance(findChurch, connections, gateway);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  void churchWith(String account) {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    var connection = new WhatsAppConnection(church, "cus", ConnectionMode.DEDICATED);
    connection.markConnected("pn", "+51 1", account, LocalDateTime.now());
    when(connections.findById(church)).thenReturn(Optional.of(connection));
  }

  @Test
  void theTemplatesTheAppSendsAreSubmittedForTheChurchsAccount() {
    churchWith("waba-1");

    var result = handler().handle(new ProvisionTemplatesCommand(node));

    assertThat(result.templates())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.name()).isEqualTo("event_reminder");
              assertThat(t.submitted()).isTrue();
            });
    verify(gateway).createTemplate("waba-1", WhatsAppTemplate.EVENT_REMINDER);
    // The welcome is not sent yet, so it is not created.
    verify(gateway, never()).createTemplate("waba-1", WhatsAppTemplate.WELCOME);
  }

  @Test
  void aTemplateMetaRefusesIsReportedAndDoesNotRaiseAnError() {
    churchWith("waba-1");
    doThrow(new PermanentDeliveryException("Kapso refused: 400 already exists"))
        .when(gateway)
        .createTemplate("waba-1", WhatsAppTemplate.EVENT_REMINDER);

    var result = handler().handle(new ProvisionTemplatesCommand(node));

    assertThat(result.templates())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.submitted()).isFalse();
              assertThat(t.detail()).contains("already exists");
            });
  }

  @Test
  void aChurchWithoutWhatsappCannotHaveTemplates() {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(connections.findById(church)).thenReturn(Optional.empty());

    var handler = handler();
    assertThatThrownBy(() -> handler.handle(new ProvisionTemplatesCommand(node)))
        .isInstanceOf(DomainValidationException.class);
    verify(gateway, never()).createTemplate(any(), any());
  }

  @Test
  void withoutKnowingTheAccountItAsksToConnectAgain() {
    churchWith(null);

    var handler = handler();
    assertThatThrownBy(() -> handler.handle(new ProvisionTemplatesCommand(node)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("vuelve a conectar");
    verify(gateway, never()).createTemplate(any(), any());
  }

  @Test
  void anUnknownNodeHasNothing() {
    when(findChurch.handle(any())).thenReturn(Optional.empty());

    var handler = handler();
    assertThatThrownBy(() -> handler.handle(new ProvisionTemplatesCommand(node)))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void everyTemplateHasAnExamplePerVariableAndItsBodyUsesThemAll() {
    for (var template : WhatsAppTemplate.values()) {
      assertThat(template.examples()).hasSize(template.variables());
      for (var i = 1; i <= template.variables(); i++) {
        assertThat(template.body()).contains("{{" + i + "}}");
      }
      assertThat(template.body()).doesNotContain("{{" + (template.variables() + 1) + "}}");
    }
  }
}
