package org.ouracademy.ovejafiel.person.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.person.domain.Person;

class PersonInfoTests {

  @Test
  void aNewPersonAcceptsWhatsappByDefault() {
    assertThat(new Person().isWhatsappConsent()).isTrue();
  }

  @Test
  void keepsTheCurrentChoiceWhenNoneIsSent() {
    var person = new Person();
    person.setWhatsappConsent(false);

    new PersonInfo("Ana", "Diaz", "999", null, null, null, null).applyTo(person);

    assertThat(person.isWhatsappConsent()).isFalse();
  }

  @Test
  void appliesTheChoiceWhenSent() {
    var person = new Person();

    new PersonInfo("Ana", "Diaz", "999", null, null, null, null, false).applyTo(person);

    assertThat(person.isWhatsappConsent()).isFalse();
  }
}
