package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.command.create_link_code.CreateLinkCodeCommand;
import org.ouracademy.ovejafiel.whatsapp.command.create_link_code.CreateLinkCodeCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppCommand;
import org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppResult.Status;
import org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp.UnlinkWhatsAppCommand;
import org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp.UnlinkWhatsAppCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.LinkCodes;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.query.get_link.GetLinkQuery;
import org.ouracademy.ovejafiel.whatsapp.query.get_link.GetLinkQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppLinkRepository;

/** Proving that a number is the person's: asking for the code, sending it, and undoing it. */
class LinkHandlersTests {

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();
  UserId user = UserId.generate();

  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  FindLinkedPersonQueryHandler findLinkedPerson = mock(FindLinkedPersonQueryHandler.class);
  FindOrganizationByIdQueryHandler findOrganization = mock(FindOrganizationByIdQueryHandler.class);
  WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  WhatsAppLinkRepository links = mock(WhatsAppLinkRepository.class);

  @SuppressWarnings("unchecked")
  static <T> T build(String className, Object... args) throws Exception {
    var constructor = Class.forName(className).getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    return (T) constructor.newInstance(args);
  }

  void signedIn() {
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(user, "auth0|ana", "ana@x.org", true)));
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  WhatsAppConnection connected() {
    var connection = new WhatsAppConnection(church, "cus-1", ConnectionMode.DEDICATED);
    connection.markConnected("pn-1", "+51 999 888 777", LocalDateTime.now());
    when(connections.findById(church)).thenReturn(Optional.of(connection));
    when(connections.findByPhoneNumberId("pn-1")).thenReturn(Optional.of(connection));
    return connection;
  }

  // --- asking for the code

  CreateLinkCodeCommandHandler create() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.command.create_link_code."
            + "CreateLinkCodeCommandHandlerImpl",
        findUser,
        findChurch,
        findLinkedPerson,
        connections,
        links);
  }

  @Test
  void aPersonOfTheChurchGetsACodeAndOnlyItsHashIsKept() throws Exception {
    signedIn();
    connected();
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(PartyId.generate())));

    var result = create().handle(new CreateLinkCodeCommand("auth0|ana", node));

    assertThat(result.code()).matches("[0-9A-Z]{4}-[0-9A-Z]{4}");
    assertThat(result.displayNumber()).isEqualTo("+51 999 888 777");
    assertThat(result.expiresAt())
        .isBetween(LocalDateTime.now().plusMinutes(9), LocalDateTime.now().plusMinutes(11));
    verify(links)
        .saveCode(
            eq(user.value()),
            eq(church.value()),
            eq(LinkCodes.hash(result.code())),
            eq(result.expiresAt()));
  }

  @Test
  void someoneWithoutAPersonInTheChurchGetsNoCode() throws Exception {
    signedIn();
    connected();
    when(findLinkedPerson.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> create().handle(new CreateLinkCodeCommand("auth0|ana", node)))
        .isInstanceOf(DomainValidationException.class);
    verify(links, never()).saveCode(any(), any(), any(), any());
  }

  @Test
  void aChurchWithoutWhatsappGivesNoCode() throws Exception {
    signedIn();
    when(connections.findById(church)).thenReturn(Optional.empty());
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(PartyId.generate())));

    assertThatThrownBy(() -> create().handle(new CreateLinkCodeCommand("auth0|ana", node)))
        .isInstanceOf(DomainValidationException.class);
  }

  // --- sending the code

  LinkWhatsAppCommandHandler link() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp.LinkWhatsAppCommandHandlerImpl",
        connections,
        links,
        findOrganization);
  }

  void churchIsFound() {
    when(findOrganization.handle(any()))
        .thenReturn(
            Optional.of(
                new FindOrganizationResult(
                    church,
                    "Iglesia Central",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    "slug",
                    null,
                    null)));
  }

  @Test
  void theRightCodeFromANumberProvesItAndIsUsedUp() throws Exception {
    connected();
    churchIsFound();
    when(links.countFailuresSince(eq("51999111222"), any())).thenReturn(0);
    when(links.findValidCode(eq(LinkCodes.hash("AB12-CD34")), eq(church.value()), any()))
        .thenReturn(Optional.of(new WhatsAppLinkRepository.Code(user.value(), church.value())));

    var result = link().handle(new LinkWhatsAppCommand("pn-1", "+51 999 111 222", "ab12 cd34"));

    assertThat(result.status()).isEqualTo(Status.LINKED);
    assertThat(result.churchName()).isEqualTo("Iglesia Central");
    verify(links).saveIdentity(eq(user.value()), eq(church.value()), eq("51999111222"), any());
    verify(links).deleteCode(user.value());
    verify(links, never()).recordFailure(any(), any());
  }

  @Test
  void aWrongOrExpiredCodeIsRefusedAndCounted() throws Exception {
    connected();
    when(links.findValidCode(any(), any(), any())).thenReturn(Optional.empty());

    var result = link().handle(new LinkWhatsAppCommand("pn-1", "51999111222", "AB12-CD34"));

    assertThat(result.status()).isEqualTo(Status.INVALID_CODE);
    verify(links).recordFailure(eq("51999111222"), any());
    verify(links, never()).saveIdentity(any(), any(), any(), any());
  }

  @Test
  void somethingThatIsNotEvenACodeIsCountedWithoutLookingItUp() throws Exception {
    connected();

    var result = link().handle(new LinkWhatsAppCommand("pn-1", "51999111222", "hola"));

    assertThat(result.status()).isEqualTo(Status.INVALID_CODE);
    verify(links).recordFailure(eq("51999111222"), any());
    verify(links, never()).findValidCode(any(), any(), any());
  }

  @Test
  void afterTooManyWrongTriesEvenTheRightCodeIsRefused() throws Exception {
    connected();
    when(links.countFailuresSince(eq("51999111222"), any())).thenReturn(5);
    when(links.findValidCode(any(), any(), any()))
        .thenReturn(Optional.of(new WhatsAppLinkRepository.Code(user.value(), church.value())));

    var result = link().handle(new LinkWhatsAppCommand("pn-1", "51999111222", "AB12-CD34"));

    assertThat(result.status()).isEqualTo(Status.RATE_LIMITED);
    verify(links, never()).saveIdentity(any(), any(), any(), any());
    verify(links, never()).findValidCode(any(), any(), any());
  }

  @Test
  void aNumberThatIsNotAConnectedChurchLinksNothing() throws Exception {
    when(connections.findByPhoneNumberId("pn-x")).thenReturn(Optional.empty());

    assertThat(link().handle(new LinkWhatsAppCommand("pn-x", "51999111222", "AB12-CD34")).status())
        .isEqualTo(Status.CHURCH_NOT_CONNECTED);
    verify(links, never()).recordFailure(any(), any());
  }

  // --- undoing and looking

  UnlinkWhatsAppCommandHandler unlink() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp."
            + "UnlinkWhatsAppCommandHandlerImpl",
        findUser,
        findChurch,
        links);
  }

  @Test
  void unlinkingRemovesTheProofOfThatAccountOnly() throws Exception {
    signedIn();
    when(links.deleteIdentity(user.value(), church.value())).thenReturn(true);

    assertThat(unlink().handle(new UnlinkWhatsAppCommand("auth0|ana", node)).wasLinked()).isTrue();
  }

  GetLinkQueryHandler get() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.query.get_link.GetLinkQueryHandlerImpl",
        findUser,
        findChurch,
        links);
  }

  @Test
  void theLinkIsShownWithTheNumberMasked() throws Exception {
    signedIn();
    var verified = LocalDateTime.now();
    when(links.findIdentity(user.value(), church.value()))
        .thenReturn(
            Optional.of(
                new WhatsAppLinkRepository.Identity(user.value(), verified, "51999111222")));

    var result = get().handle(new GetLinkQuery("auth0|ana", node));

    assertThat(result.linked()).isTrue();
    assertThat(result.maskedPhone()).isEqualTo("+•••••••1222");
    assertThat(result.maskedPhone()).doesNotContain("51999");
    assertThat(result.verifiedAt()).isEqualTo(verified);
  }

  @Test
  void withoutALinkItSaysSo() throws Exception {
    signedIn();
    when(links.findIdentity(any(), any())).thenReturn(Optional.empty());

    var result = get().handle(new GetLinkQuery("auth0|ana", node));

    assertThat(result.linked()).isFalse();
    assertThat(result.maskedPhone()).isNull();
  }
}
