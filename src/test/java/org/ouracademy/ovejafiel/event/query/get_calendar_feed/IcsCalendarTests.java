package org.ouracademy.ovejafiel.event.query.get_calendar_feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcsCalendarTests {

  static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private static IcsCalendar.Entry entry(String summary, String location, String description) {
    return new IcsCalendar.Entry(
        "id-1@srtomy",
        Instant.parse("2026-01-05T15:00:00Z"),
        Instant.parse("2026-01-05T17:00:00Z"),
        summary,
        location,
        description,
        "https://app.example/portal/1/event/2/attendance");
  }

  @Test
  void writesAValidCalendarWithCrlfLineEnds() {
    var ics = IcsCalendar.write("Sr. Tommy", List.of(entry("Culto", "Av. 1", "Hola")), NOW);

    assertThat(ics).startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n").endsWith("END:VCALENDAR\r\n");
    assertThat(ics.replace("\r\n", "")).doesNotContain("\n");
    assertThat(ics)
        .contains(
            "BEGIN:VEVENT\r\n",
            "UID:id-1@srtomy\r\n",
            "DTSTAMP:20260101T000000Z\r\n",
            "DTSTART:20260105T150000Z\r\n",
            "DTEND:20260105T170000Z\r\n",
            "SUMMARY:Culto\r\n",
            "LOCATION:Av. 1\r\n",
            "DESCRIPTION:Hola\r\n",
            "URL:https://app.example/portal/1/event/2/attendance\r\n",
            "END:VEVENT\r\n");
  }

  @Test
  void anEmptyCalendarHasNoEvents() {
    var ics = IcsCalendar.write("Sr. Tommy", List.of(), NOW);

    assertThat(ics).doesNotContain("BEGIN:VEVENT");
    assertThat(ics).contains("X-WR-CALNAME:Sr. Tommy\r\n");
  }

  @Test
  void leavesOutTheOptionalFieldsThatAreMissing() {
    var ics = IcsCalendar.write("Sr. Tommy", List.of(entry("Culto", null, " ")), NOW);

    assertThat(ics).doesNotContain("LOCATION").doesNotContain("DESCRIPTION");
  }

  @Test
  void escapesBackslashesSemicolonsCommasAndNewlines() {
    var backslash = "\\";
    assertThat(IcsCalendar.escape("a" + backslash + "b;c,d\ne\r\nf"))
        .isEqualTo(
            "a" + backslash + backslash + "b" + backslash + ";c" + backslash + ",d" + backslash
                + "ne" + backslash + "nf");
  }

  @Test
  void foldsLongLinesAt75OctetsWithoutSplittingACharacter() {
    var ics = IcsCalendar.write("Sr. Tommy", List.of(entry("ñ".repeat(100), null, null)), NOW);

    for (var line : ics.split("\r\n")) {
      assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
    }
    // Unfolding gives back the original text.
    assertThat(ics.replace("\r\n ", "")).contains("SUMMARY:" + "ñ".repeat(100));
  }
}
