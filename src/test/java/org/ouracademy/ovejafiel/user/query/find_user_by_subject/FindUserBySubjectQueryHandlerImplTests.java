package org.ouracademy.ovejafiel.user.query.find_user_by_subject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.repository.UserRepository;

class FindUserBySubjectQueryHandlerImplTests {

  UserRepository repository = mock(UserRepository.class);
  FindUserBySubjectQueryHandlerImpl handler = new FindUserBySubjectQueryHandlerImpl(repository);

  @Test
  void findsTheUserOrNothing() {
    when(repository.findBySubject("a")).thenReturn(Optional.of(new AppUser("a")));

    assertThat(handler.handle(new FindUserBySubjectQuery("a"))).isPresent();
    assertThat(handler.handle(new FindUserBySubjectQuery("b"))).isEmpty();
  }
}
