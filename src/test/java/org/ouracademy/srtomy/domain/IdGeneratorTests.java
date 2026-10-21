package org.ouracademy.srtomy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdGeneratorTests {

  @Test
  void newUuidV7GeneratesAVersion7Uuid() {
    var uuid = IdGenerator.newUuidV7();

    assertThat(uuid.version()).isEqualTo(7);
  }

  @Test
  void newUuidV7GeneratesDistinctValues() {
    assertThat(IdGenerator.newUuidV7()).isNotEqualTo(IdGenerator.newUuidV7());
  }
}
