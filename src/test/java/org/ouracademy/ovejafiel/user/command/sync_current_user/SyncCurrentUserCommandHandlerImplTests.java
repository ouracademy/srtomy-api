package org.ouracademy.ovejafiel.user.command.sync_current_user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SyncCurrentUserCommandHandlerImplTests {

  @Mock UserRepository userRepository;
  @InjectMocks SyncCurrentUserCommandHandlerImpl handler;

  @Test
  void aNewSubjectIsRegisteredWithItsEmail() {
    when(userRepository.findBySubject("auth0|1")).thenReturn(Optional.empty());
    when(userRepository.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));

    var result = handler.handle(new SyncCurrentUserCommand("auth0|1", "ana@x.org", true));

    assertThat(result.created()).isTrue();
    assertThat(result.user().subject()).isEqualTo("auth0|1");
    assertThat(result.user().email()).isEqualTo("ana@x.org");
    assertThat(result.user().emailVerified()).isTrue();
  }

  @Test
  void aKnownSubjectKeepsItsIdAndItsEmailWhenTheTokenHasNone() {
    var existing = new AppUser("auth0|1");
    existing.updateContact("ana@x.org", true);
    when(userRepository.findBySubject("auth0|1")).thenReturn(Optional.of(existing));
    when(userRepository.save(existing)).thenReturn(existing);

    var result = handler.handle(new SyncCurrentUserCommand("auth0|1", null, false));

    assertThat(result.created()).isFalse();
    assertThat(result.user().id()).isEqualTo(existing.getId());
    assertThat(result.user().email()).isEqualTo("ana@x.org");
  }
}
