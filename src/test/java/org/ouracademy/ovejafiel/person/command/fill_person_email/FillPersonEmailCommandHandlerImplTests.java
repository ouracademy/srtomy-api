package org.ouracademy.ovejafiel.person.command.fill_person_email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

class FillPersonEmailCommandHandlerImplTests {

  PersonRepository repository = mock(PersonRepository.class);
  FillPersonEmailCommandHandlerImpl handler = new FillPersonEmailCommandHandlerImpl(repository);

  PartyId id = PartyId.generate();

  @Test
  void givesAnEmailToAPersonWithoutOne() {
    var person = new Person();
    when(repository.findById(id)).thenReturn(Optional.of(person));

    var result = handler.handle(new FillPersonEmailCommand(id, " Ana@X.org "));

    assertThat(result.filled()).isTrue();
    assertThat(person.getEmail()).isEqualTo("ana@x.org");
  }

  @Test
  void aBlankEmailCountsAsNone() {
    var person = new Person();
    person.setEmail("  ");
    when(repository.findById(id)).thenReturn(Optional.of(person));

    assertThat(handler.handle(new FillPersonEmailCommand(id, "ana@x.org")).filled()).isTrue();
  }

  @Test
  void neverOverwritesAnEmailThatIsThere() {
    var person = new Person();
    person.setEmail("personal@x.org");
    when(repository.findById(id)).thenReturn(Optional.of(person));

    var result = handler.handle(new FillPersonEmailCommand(id, "work@x.org"));

    assertThat(result.filled()).isFalse();
    assertThat(person.getEmail()).isEqualTo("personal@x.org");
  }

  @Test
  void anUnknownPersonIsNotFound() {
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new FillPersonEmailCommand(id, "a@x.org")))
        .isInstanceOf(NoSuchElementException.class);
  }
}
