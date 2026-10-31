package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommand;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionResult;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommand;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommand;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionStatus;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQuery;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionResult.State;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;

/** The handlers of the module, with the repositories and the provider replaced by mocks. */
class WhatsAppCommandHandlersTests {

  private final PartyId church = PartyId.generate();
  private final PartyId branch = PartyId.generate();

  private final WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  private final WhatsAppWebhookEventRepository webhookEvents =
      mock(WhatsAppWebhookEventRepository.class);
  private final WhatsAppGateway gateway = mock(WhatsAppGateway.class);
  private final FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  private final FindOrganizationByIdQueryHandler findOrganization =
      mock(FindOrganizationByIdQueryHandler.class);

  private WhatsAppConnection existing(ConnectionMode mode) {
    var connection = new WhatsAppConnection(church, "cus-1", mode);
    when(connections.findById(church)).thenReturn(Optional.of(connection));
    return connection;
  }

  private void churchOf(PartyId node) {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  // --- request setup link

  private RequestSetupLinkCommandHandler requestHandler() {
    return handler(
        "org.ouracademy.ovejafiel.whatsapp.command.request_setup_link."
            + "RequestSetupLinkCommandHandlerImpl",
        connections,
        gateway,
        findChurch,
        findOrganization);
  }

  private <T> T handler(String className, Object... args) {
    try {
      var type = Class.forName(className);
      var constructor = type.getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      @SuppressWarnings("unchecked")
      var instance = (T) constructor.newInstance(args);
      return instance;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void theFirstLinkCreatesTheCustomerOfTheChurchAndKeepsItPending() {
    churchOf(branch);
    when(connections.findById(church)).thenReturn(Optional.empty());
    when(findOrganization.handle(any()))
        .thenReturn(Optional.of(organizationNamed("Iglesia Central")));
    when(gateway.createCustomer(church.value().toString(), "Iglesia Central")).thenReturn("cus-9");
    var expires = Instant.parse("2026-12-01T00:00:00Z");
    when(gateway.createSetupLink("cus-9", ConnectionMode.COEXISTENCE, "es"))
        .thenReturn(new WhatsAppGateway.SetupLink("https://setup/x", expires));

    var result =
        requestHandler().handle(new RequestSetupLinkCommand(branch, ConnectionMode.COEXISTENCE));

    assertThat(result.url()).isEqualTo("https://setup/x");
    assertThat(result.expiresAt()).isEqualTo(expires);
    var saved = org.mockito.ArgumentCaptor.forClass(WhatsAppConnection.class);
    verify(connections).save(saved.capture());
    assertThat(saved.getValue().getOrganizationId()).isEqualTo(church);
    assertThat(saved.getValue().getKapsoCustomerId()).isEqualTo("cus-9");
    assertThat(saved.getValue().getStatus()).isEqualTo(ConnectionStatus.PENDING);
  }

  @Test
  void aNewLinkReusesTheCustomerAndSendsAConnectedChurchBackToPending() {
    churchOf(church);
    var connection = existing(ConnectionMode.DEDICATED);
    connection.markConnected("pn", "+51", LocalDateTime.now());
    when(gateway.createSetupLink("cus-1", ConnectionMode.COEXISTENCE, "es"))
        .thenReturn(new WhatsAppGateway.SetupLink("https://setup/y", Instant.now()));

    requestHandler().handle(new RequestSetupLinkCommand(church, ConnectionMode.COEXISTENCE));

    verify(gateway, never()).createCustomer(any(), any());
    assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.PENDING);
    assertThat(connection.getMode()).isEqualTo(ConnectionMode.COEXISTENCE);
  }

  @Test
  void anUnknownNodeHasNoLink() {
    when(findChurch.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                requestHandler()
                    .handle(new RequestSetupLinkCommand(branch, ConnectionMode.DEDICATED)))
        .isInstanceOf(NoSuchElementException.class);
  }

  // --- confirm connection

  private ConfirmConnectionCommandHandler confirmHandler() {
    return handler(
        "org.ouracademy.ovejafiel.whatsapp.command.confirm_connection."
            + "ConfirmConnectionCommandHandlerImpl",
        connections,
        webhookEvents);
  }

  @Test
  void theWebhookConnectsTheChurchOfTheCustomer() {
    var connection = new WhatsAppConnection(church, "cus-1", ConnectionMode.DEDICATED);
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    when(connections.findByKapsoCustomerId("cus-1")).thenReturn(Optional.of(connection));

    var result = confirmHandler().handle(new ConfirmConnectionCommand("k1", "cus-1", "pn-7"));

    assertThat(result).isEqualTo(ConfirmConnectionResult.CONNECTED);
    assertThat(connection.isConnected()).isTrue();
    assertThat(connection.getPhoneNumberId()).isEqualTo("pn-7");
    assertThat(connection.getConnectedAt()).isNotNull();
    verify(connections).save(connection);
  }

  @Test
  void aRepeatedDeliveryChangesNothing() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(false);

    var result = confirmHandler().handle(new ConfirmConnectionCommand("k1", "cus-1", "pn-7"));

    assertThat(result).isEqualTo(ConfirmConnectionResult.ALREADY_HANDLED);
    verify(connections, never()).save(any());
  }

  @Test
  void aCustomerThatIsNotOursIsIgnored() {
    when(webhookEvents.markReceived(any(), any())).thenReturn(true);
    when(connections.findByKapsoCustomerId("zzz")).thenReturn(Optional.empty());

    var result = confirmHandler().handle(new ConfirmConnectionCommand("k1", "zzz", "pn"));

    assertThat(result).isEqualTo(ConfirmConnectionResult.UNKNOWN_CUSTOMER);
    verify(connections, never()).save(any());
  }

  @Test
  void withoutAnIdempotencyKeyItStillConnects() {
    var connection = new WhatsAppConnection(church, "cus-1", ConnectionMode.DEDICATED);
    when(connections.findByKapsoCustomerId("cus-1")).thenReturn(Optional.of(connection));

    var result = confirmHandler().handle(new ConfirmConnectionCommand(null, "cus-1", "pn"));

    assertThat(result).isEqualTo(ConfirmConnectionResult.CONNECTED);
    verify(webhookEvents, never()).markReceived(any(), any());
  }

  // --- disconnect

  private DisconnectCommandHandler disconnectHandler() {
    return handler(
        "org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommandHandlerImpl",
        connections,
        findChurch);
  }

  @Test
  void disconnectingACommonConnectionMarksItAndSaysItWasConnected() {
    churchOf(branch);
    var connection = existing(ConnectionMode.DEDICATED);
    connection.markConnected("pn", "+51", LocalDateTime.now());

    var result = disconnectHandler().handle(new DisconnectCommand(branch));

    assertThat(result.wasConnected()).isTrue();
    assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);
    verify(connections).save(connection);
  }

  @Test
  void aChurchThatNeverConnectedCannotBeDisconnected() {
    churchOf(church);
    when(connections.findById(church)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> disconnectHandler().handle(new DisconnectCommand(church)))
        .isInstanceOf(NoSuchElementException.class);
  }

  // --- get connection

  private GetConnectionQueryHandler queryHandler() {
    return handler(
        "org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQueryHandlerImpl",
        connections,
        findChurch);
  }

  @Test
  void aBranchSeesTheConnectionOfItsChurchAsInherited() {
    churchOf(branch);
    var connection = existing(ConnectionMode.COEXISTENCE);
    connection.markConnected("pn", "+51 1", LocalDateTime.now());

    var result = queryHandler().handle(new GetConnectionQuery(branch));

    assertThat(result.state()).isEqualTo(State.CONNECTED);
    assertThat(result.inherited()).isTrue();
    assertThat(result.churchId()).isEqualTo(church);
    assertThat(result.displayNumber()).isEqualTo("+51 1");
  }

  @Test
  void theChurchItselfIsNotInherited() {
    churchOf(church);
    existing(ConnectionMode.DEDICATED);

    var result = queryHandler().handle(new GetConnectionQuery(church));

    assertThat(result.state()).isEqualTo(State.PENDING);
    assertThat(result.inherited()).isFalse();
  }

  @Test
  void aChurchWithoutConnectionIsNotConnected() {
    churchOf(church);
    when(connections.findById(church)).thenReturn(Optional.empty());

    var result = queryHandler().handle(new GetConnectionQuery(church));

    assertThat(result.state()).isEqualTo(State.NOT_CONNECTED);
    assertThat(result.mode()).isNull();
  }

  private FindOrganizationResult organizationNamed(String name) {
    return new FindOrganizationResult(
        church, name, null, null, null, null, null, null, null, null, false, null, null, null);
  }
}
