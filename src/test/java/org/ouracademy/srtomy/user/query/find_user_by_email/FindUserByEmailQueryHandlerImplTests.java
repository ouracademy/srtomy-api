package org.ouracademy.srtomy.user.query.find_user_by_email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.repository.UserRepository;

class FindUserByEmailQueryHandlerImplTests {

  UserRepository repository = mock(UserRepository.class);
  FindUserByEmailQueryHandlerImpl handler = new FindUserByEmailQueryHandlerImpl(repository);

  @Test
  void findsAVerifiedUserIgnoringCaseAndSurroundingSpaces() {
    var user = new AppUser("auth0|1");
    user.updateContact("ana@x.org", true);
    when(repository.findFirstByEmailIgnoreCaseAndEmailVerifiedTrueOrderByCreateDate("Ana@x.org"))
        .thenReturn(Optional.of(user));

    assertThat(handler.handle(new FindUserByEmailQuery(" Ana@x.org ")))
        .hasValueSatisfying(result -> assertThat(result.subject()).isEqualTo("auth0|1"));
  }

  @Test
  void aBlankEmailMatchesNobodyWithoutTouchingTheDatabase() {
    assertThat(handler.handle(new FindUserByEmailQuery(" "))).isEmpty();
    assertThat(handler.handle(new FindUserByEmailQuery(null))).isEmpty();
    verifyNoInteractions(repository);
  }
}
