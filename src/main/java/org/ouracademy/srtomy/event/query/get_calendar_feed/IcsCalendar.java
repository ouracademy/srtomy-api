package org.ouracademy.srtomy.event.query.get_calendar_feed;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes an iCalendar (RFC 5545) file: CRLF line ends, lines folded at 75 octets and text escaped.
 */
final class IcsCalendar {

  private static final DateTimeFormatter UTC =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  /**
   * An event of the calendar; {@code location}, {@code description} and {@code url} may be null.
   */
  record Entry(
      String uid,
      Instant start,
      Instant end,
      String summary,
      String location,
      String description,
      String url) {}

  private IcsCalendar() {}

  static String write(String name, List<Entry> entries, Instant now) {
    var out = new StringBuilder();
    line(out, "BEGIN:VCALENDAR");
    line(out, "VERSION:2.0");
    line(out, "PRODID:-//Sr. Tommy//Eventos//ES");
    line(out, "CALSCALE:GREGORIAN");
    line(out, "METHOD:PUBLISH");
    line(out, "X-WR-CALNAME:" + escape(name));
    // Hints for the apps that honor them; Google Calendar decides its own pace.
    line(out, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
    line(out, "X-PUBLISHED-TTL:PT1H");
    for (var entry : entries) {
      line(out, "BEGIN:VEVENT");
      line(out, "UID:" + entry.uid());
      line(out, "DTSTAMP:" + UTC.format(now));
      line(out, "DTSTART:" + UTC.format(entry.start()));
      line(out, "DTEND:" + UTC.format(entry.end()));
      line(out, "SUMMARY:" + escape(entry.summary()));
      optional(out, "LOCATION", entry.location());
      optional(out, "DESCRIPTION", entry.description());
      if (entry.url() != null) {
        line(out, "URL:" + entry.url());
      }
      line(out, "END:VEVENT");
    }
    line(out, "END:VCALENDAR");
    return out.toString();
  }

  private static void optional(StringBuilder out, String property, String value) {
    if (value != null && !value.isBlank()) {
      line(out, property + ":" + escape(value));
    }
  }

  static String escape(String text) {
    return text.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n");
  }

  /** Appends the line folded every 75 octets, never inside a multi-byte character. */
  private static void line(StringBuilder out, String content) {
    var octets = 0;
    var limit = 75;
    for (var i = 0; i < content.length(); ) {
      var codePoint = content.codePointAt(i);
      var size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
      if (octets + size > limit) {
        out.append("\r\n ");
        octets = 0;
        limit = 74; // the folding space takes one octet
      }
      out.appendCodePoint(codePoint);
      octets += size;
      i += Character.charCount(codePoint);
    }
    out.append("\r\n");
  }
}
