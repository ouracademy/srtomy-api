package org.ouracademy.ovejafiel.whatsapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class LinkCodesTests {

  @Test
  void aCodeHasTheShapeXxxxDashXxxxAndOnlyTheAllowedLetters() {
    for (var i = 0; i < 200; i++) {
      assertThat(LinkCodes.generate()).matches("[0-9A-HJKMNP-TV-Z]{4}-[0-9A-HJKMNP-TV-Z]{4}");
    }
  }

  @Test
  void codesDoNotRepeat() {
    var seen = new HashSet<String>();
    for (var i = 0; i < 1000; i++) {
      seen.add(LinkCodes.generate());
    }
    assertThat(seen).hasSize(1000);
  }

  @Test
  void whatPeopleTypeIsReadTheWayCrockfordDoes() {
    assertThat(LinkCodes.normalize("ab12-cd34")).isEqualTo("AB12CD34");
    assertThat(LinkCodes.normalize(" ab12 cd34 ")).isEqualTo("AB12CD34");
    assertThat(LinkCodes.normalize("O0IL")).isEqualTo("0011");
    assertThat(LinkCodes.normalize(null)).isEmpty();
  }

  @Test
  void differentWaysToTypeTheSameCodeHaveTheSameHash() {
    assertThat(LinkCodes.hash("ab12-cd34")).isEqualTo(LinkCodes.hash("AB12CD34"));
    assertThat(LinkCodes.hash("AB12-CD34")).isNotEqualTo(LinkCodes.hash("AB12-CD35"));
    assertThat(LinkCodes.hash("AB12-CD34")).hasSize(64).doesNotContain("AB12");
  }

  @Test
  void onlyEightCharactersOfTheAlphabetLookLikeACode() {
    assertThat(LinkCodes.looksLikeACode("AB12-CD34")).isTrue();
    assertThat(LinkCodes.looksLikeACode("ab12cd34")).isTrue();
    assertThat(LinkCodes.looksLikeACode("AB12-CD3")).isFalse();
    assertThat(LinkCodes.looksLikeACode("AB12-CD345")).isFalse();
    assertThat(LinkCodes.looksLikeACode("AB12-CD3U")).isFalse();
    assertThat(LinkCodes.looksLikeACode("hola")).isFalse();
    assertThat(LinkCodes.looksLikeACode(null)).isFalse();
  }

  @Test
  void aPhoneIsComparedByItsDigits() {
    assertThat(LinkCodes.digitsOf("+51 999-888-777")).isEqualTo("51999888777");
    assertThat(LinkCodes.digitsOf(null)).isEmpty();
  }
}
