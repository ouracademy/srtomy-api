package org.ouracademy.ovejafiel.whatsapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumbersTests {

  @Test
  void aNumberWithItsCountryCodeIsInternational() {
    assertThat(PhoneNumbers.isInternational("+51999888777")).isTrue();
    assertThat(PhoneNumbers.isInternational("+51 999 888 777")).isTrue();
    assertThat(PhoneNumbers.isInternational("  +57 300 123 4567 ")).isTrue();
  }

  @Test
  void aNumberWithoutThePlusIsNotInternationalWhateverItLooksLike() {
    assertThat(PhoneNumbers.isInternational("999888777")).isFalse();
    assertThat(PhoneNumbers.isInternational("51999888777")).isFalse();
    assertThat(PhoneNumbers.isInternational("0051999888777")).isFalse();
  }

  @Test
  void aPlusWithTooFewDigitsIsNotAPhone() {
    assertThat(PhoneNumbers.isInternational("+51")).isFalse();
    assertThat(PhoneNumbers.isInternational("+")).isFalse();
  }

  @Test
  void nothingIsNotAPhone() {
    assertThat(PhoneNumbers.isInternational(null)).isFalse();
    assertThat(PhoneNumbers.isInternational("")).isFalse();
    assertThat(PhoneNumbers.isInternational("   ")).isFalse();
  }
}
