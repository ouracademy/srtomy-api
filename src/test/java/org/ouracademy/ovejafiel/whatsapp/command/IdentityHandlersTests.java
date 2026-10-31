package org.ouracademy.ovejafiel.whatsapp.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.set_whatsapp_consent.SetWhatsAppConsentCommand;
import org.ouracademy.ovejafiel.person.command.set_whatsapp_consent.SetWhatsAppConsentCommandHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneResult;
import org.ouracademy.ovejafiel.shared.DelegatedTokens;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommand;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserCommandHandler;
import org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user.ResolveWhatsAppUserResult;
import org.ouracademy.ovejafiel.whatsapp.command.opt_out.OptOutCommand;
import org.ouracademy.ovejafiel.whatsapp.command.opt_out.OptOutCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityCommand;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityResult.Status;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppConnection;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.beans.factory.ObjectProvider;

/** Who writes, and who asks to stop: the two ways in for the WhatsApp agent. */
class IdentityHandlersTests {

  PartyId church = PartyId.generate();
  PartyId ana = PartyId.generate();

  WhatsAppConnectionRepository connections = mock(WhatsAppConnectionRepository.class);
  FindPeopleByPhoneQueryHandler findPeople = mock(FindPeopleByPhoneQueryHandler.class);
  ResolveWhatsAppUserCommandHandler resolveUser = mock(ResolveWhatsAppUserCommandHandler.class);
  SetWhatsAppConsentCommandHandler setConsent = mock(SetWhatsAppConsentCommandHandler.class);
  DelegatedTokens tokens;

  @SuppressWarnings("unchecked")
  ObjectProvider<DelegatedTokens> provider = mock(ObjectProvider.class);

  void keyConfigured() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    tokens =
        DelegatedTokens.of(
            new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyIDFromThumbprint()
                .build());
    when(provider.getIfAvailable()).thenReturn(tokens);
  }

  ResolveIdentityCommandHandler resolver() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.command.resolve_identity."
            + "ResolveIdentityCommandHandlerImpl",
        connections,
        findPeople,
        resolveUser,
        provider);
  }

  OptOutCommandHandler optOut() throws Exception {
    return build(
        "org.ouracademy.ovejafiel.whatsapp.command.opt_out.OptOutCommandHandlerImpl",
        connections,
        findPeople,
        setConsent);
  }

  @SuppressWarnings("unchecked")
  static <T> T build(String className, Object... args) throws Exception {
    Constructor<?> constructor = Class.forName(className).getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    return (T) constructor.newInstance(args);
  }

  void connected() {
    var connection = new WhatsAppConnection(church, "cus-1", ConnectionMode.DEDICATED);
    connection.markConnected("pn-1", "+51 1", LocalDateTime.now());
    when(connections.findByPhoneNumberId("pn-1")).thenReturn(Optional.of(connection));
  }

  void peopleWithThatPhone(FindPeopleByPhoneResult.Match... matches) {
    when(findPeople.handle(new FindPeopleByPhoneQuery("51999888777", church)))
        .thenReturn(new FindPeopleByPhoneResult(List.of(matches)));
  }

  ResolveIdentityCommand writes() {
    return new ResolveIdentityCommand("pn-1", "51999888777");
  }

  // --- resolve identity

  @Test
  void aKnownPersonGetsAShortTokenToActAsThemAtMemberLevel() throws Exception {
    keyConfigured();
    connected();
    peopleWithThatPhone(new FindPeopleByPhoneResult.Match(ana, "Ana Díaz"));
    when(resolveUser.handle(new ResolveWhatsAppUserCommand(ana, church)))
        .thenReturn(new ResolveWhatsAppUserResult("whatsapp:" + ana.value(), false));

    var result = resolver().handle(writes());

    assertThat(result.status()).isEqualTo(Status.LINKED);
    assertThat(result.name()).isEqualTo("Ana Díaz");
    assertThat(result.personId()).isEqualTo(ana.value());
    assertThat(result.churchId()).isEqualTo(church.value());
    var jwt = tokens.decoder().decode(result.token());
    assertThat(jwt.getSubject()).isEqualTo("whatsapp:" + ana.value());
    assertThat(result.expiresAt()).isEqualTo(jwt.getExpiresAt());
  }

  @Test
  void aNumberThatIsNotAConnectedChurchGetsNothing() throws Exception {
    keyConfigured();
    when(connections.findByPhoneNumberId("pn-1")).thenReturn(Optional.empty());

    var result = resolver().handle(writes());

    assertThat(result.status()).isEqualTo(Status.CHURCH_NOT_CONNECTED);
    assertThat(result.token()).isNull();
    verifyNoInteractions(findPeople, resolveUser);
  }

  @Test
  void aDisconnectedChurchGetsNothing() throws Exception {
    keyConfigured();
    var connection = new WhatsAppConnection(church, "cus-1", ConnectionMode.DEDICATED);
    connection.markConnected("pn-1", "+51 1", LocalDateTime.now());
    connection.disconnect();
    when(connections.findByPhoneNumberId("pn-1")).thenReturn(Optional.of(connection));

    assertThat(resolver().handle(writes()).status()).isEqualTo(Status.CHURCH_NOT_CONNECTED);
    verifyNoInteractions(findPeople, resolveUser);
  }

  @Test
  void nobodyWithThatPhoneIsUnknownAndNothingIsCreated() throws Exception {
    keyConfigured();
    connected();
    peopleWithThatPhone();

    var result = resolver().handle(writes());

    assertThat(result.status()).isEqualTo(Status.UNKNOWN);
    assertThat(result.token()).isNull();
    verifyNoInteractions(resolveUser);
  }

  @Test
  void severalPeopleWithThatPhoneAreAmbiguousAndNoOneIsChosen() throws Exception {
    keyConfigured();
    connected();
    peopleWithThatPhone(
        new FindPeopleByPhoneResult.Match(ana, "Ana Díaz"),
        new FindPeopleByPhoneResult.Match(PartyId.generate(), "Luis Díaz"));

    var result = resolver().handle(writes());

    assertThat(result.status()).isEqualTo(Status.AMBIGUOUS);
    assertThat(result.token()).isNull();
    assertThat(result.name()).isNull();
    verifyNoInteractions(resolveUser);
  }

  @Test
  void aPersonWithARealAccountGetsNoTokenByPhoneAlone() throws Exception {
    keyConfigured();
    connected();
    peopleWithThatPhone(new FindPeopleByPhoneResult.Match(ana, "Ana Díaz"));
    when(resolveUser.handle(any())).thenReturn(new ResolveWhatsAppUserResult("auth0|ana", true));

    var result = resolver().handle(writes());

    assertThat(result.status()).isEqualTo(Status.NEEDS_VERIFICATION);
    assertThat(result.token()).isNull();
  }

  @Test
  void withoutTheSigningKeyNoTokenIsMade() throws Exception {
    connected();
    peopleWithThatPhone(new FindPeopleByPhoneResult.Match(ana, "Ana Díaz"));
    when(resolveUser.handle(any()))
        .thenReturn(new ResolveWhatsAppUserResult("whatsapp:" + ana.value(), false));
    when(provider.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(() -> resolver().handle(writes())).isInstanceOf(IllegalStateException.class);
  }

  // --- opt out

  @Test
  void stopTurnsConsentOffForEveryoneWithThatPhone() throws Exception {
    connected();
    var luis = PartyId.generate();
    peopleWithThatPhone(
        new FindPeopleByPhoneResult.Match(ana, "Ana"),
        new FindPeopleByPhoneResult.Match(luis, "Luis"));

    var result = optOut().handle(new OptOutCommand("pn-1", "51999888777"));

    assertThat(result.people()).isEqualTo(2);
    verify(setConsent).handle(new SetWhatsAppConsentCommand(List.of(ana, luis), false));
  }

  @Test
  void stopFromAnUnknownChurchNumberDoesNothing() throws Exception {
    when(connections.findByPhoneNumberId("pn-x")).thenReturn(Optional.empty());

    assertThat(optOut().handle(new OptOutCommand("pn-x", "51999888777")).people()).isZero();
    verifyNoInteractions(findPeople, setConsent);
  }
}
