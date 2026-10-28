package org.ouracademy.ovejafiel.attendance.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.EventId;

class AttendanceQrTokenTests {

  AttendanceQrToken token = new AttendanceQrToken("a-long-enough-test-secret");
  EventId event = EventId.generate();
  Instant now = Instant.parse("2026-03-01T10:02:00Z");

  @Test
  void aCodeTellsWhichEventItIsForAndIsFreshRightAway() {
    var code = token.issue(event, now);

    var verified = token.verify(code, now).orElseThrow();

    assertThat(verified.eventId()).isEqualTo(event);
    assertThat(verified.fresh()).isTrue();
  }

  @Test
  void aCodeIsUrlSafe() {
    assertThat(token.issue(event, now)).matches("[A-Za-z0-9_.-]+");
  }

  @Test
  void itKeepsWorkingInTheNextSlotButNotAfter() {
    var code = token.issue(event, now);

    assertThat(token.verify(code, now.plus(AttendanceQrToken.SLOT)).orElseThrow().fresh()).isTrue();
    assertThat(
            token
                .verify(code, now.plus(AttendanceQrToken.SLOT.multipliedBy(2)))
                .orElseThrow()
                .fresh())
        .isFalse();
  }

  @Test
  void aCodeFromTheFutureIsNotFresh() {
    var code = token.issue(event, now.plus(AttendanceQrToken.SLOT.multipliedBy(3)));

    assertThat(token.verify(code, now).orElseThrow().fresh()).isFalse();
  }

  @Test
  void theRefreshTimeIsTheStartOfTheNextSlot() {
    assertThat(token.refreshAt(now)).isEqualTo(Instant.parse("2026-03-01T10:05:00Z"));
    assertThat(token.refreshAt(Instant.parse("2026-03-01T10:05:00Z")))
        .isEqualTo(Instant.parse("2026-03-01T10:10:00Z"));
  }

  @Test
  void aCodeForAnotherEventCannotBeMadeByChangingIt() {
    var code = token.issue(event, now);
    var other = token.issue(EventId.generate(), now);
    var forged = other.split("\\.")[0] + "." + code.split("\\.")[1] + "." + code.split("\\.")[2];

    assertThat(token.verify(forged, now)).isEmpty();
  }

  @Test
  void aChangedSlotOrSignatureIsRejected() {
    var parts = token.issue(event, now).split("\\.");

    assertThat(token.verify(parts[0] + "." + (Long.parseLong(parts[1]) + 1) + "." + parts[2], now))
        .isEmpty();
    assertThat(token.verify(parts[0] + "." + parts[1] + ".AAAAAAAAAAAAAAAAAAAAAA", now)).isEmpty();
  }

  @Test
  void aCodeMadeWithAnotherSecretIsRejected() {
    var code = new AttendanceQrToken("some-other-secret-value").issue(event, now);

    assertThat(token.verify(code, now)).isEmpty();
  }

  @Test
  void garbageIsRejectedWithoutErrors() {
    for (var garbage :
        new String[] {null, "", "x", "a.b", "a.b.c", "!!.1.!!", "....", "AAAA.x.AAAA"}) {
      assertThat(token.verify(garbage, now)).isEmpty();
    }
  }

  @Test
  void withoutASecretNothingCanBeMadeOrChecked() {
    var unsigned = new AttendanceQrToken("");

    assertThatThrownBy(() -> unsigned.issue(event, now)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> unsigned.verify(token.issue(event, now), now))
        .isInstanceOf(IllegalStateException.class);
  }
}
