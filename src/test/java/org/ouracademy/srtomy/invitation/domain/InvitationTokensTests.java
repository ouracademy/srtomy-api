package org.ouracademy.srtomy.invitation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvitationTokensTests {

  @Test
  void tokensAreRandomAndUrlSafe() {
    var first = InvitationTokens.newToken();

    assertThat(first).isNotEqualTo(InvitationTokens.newToken()).matches("[A-Za-z0-9_-]{43}");
  }

  @Test
  void theHashIsStableAndDoesNotRevealTheToken() {
    var hash = InvitationTokens.hash("abc");

    assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    assertThat(InvitationTokens.hash("abc")).isEqualTo(hash);
    assertThat(InvitationTokens.hash("abd")).isNotEqualTo(hash);
  }
}
