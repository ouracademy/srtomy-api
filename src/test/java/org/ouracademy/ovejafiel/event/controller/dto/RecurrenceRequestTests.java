package org.ouracademy.ovejafiel.event.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Recurrence;

class RecurrenceRequestTests {

  @Test
  void whatIsNotSaidMeansEveryTimeAndNeverEnds() {
    var request =
        new RecurrenceRequest(Recurrence.Frequency.DAILY, null, null, null, null, null, null);

    assertThat(request.interval()).isEqualTo(1);
    assertThat(request.daysOfWeek()).isEmpty();
    assertThat(request.endType()).isEqualTo(Recurrence.EndType.NEVER);
  }

  @Test
  void whatIsSaidIsKept() {
    var request =
        new RecurrenceRequest(
            Recurrence.Frequency.WEEKLY, 2, List.of(0, 3), null, Recurrence.EndType.COUNT, null, 5);

    assertThat(request.interval()).isEqualTo(2);
    assertThat(request.daysOfWeek()).containsExactly(0, 3);
    assertThat(request.endType()).isEqualTo(Recurrence.EndType.COUNT);
    assertThat(request.count()).isEqualTo(5);
  }
}
