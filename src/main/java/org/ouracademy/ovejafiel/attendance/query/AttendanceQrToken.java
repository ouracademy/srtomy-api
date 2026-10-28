package org.ouracademy.ovejafiel.attendance.query;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The code in the QR of an event, which people scan to mark their own attendance. It is signed with
 * HMAC, so nobody can make one for an event, and it carries the 5-minute slot it was made in: a
 * photo of the screen stops working soon. A scan still counts in the slot after the one the code
 * was made in, so one that rolls over while someone is pointing their phone is not lost.
 */
@Component
public class AttendanceQrToken {

  public static final Duration SLOT = Duration.ofMinutes(5);

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
  private static final int MAC_BYTES = 16;

  private final byte[] secret;

  public AttendanceQrToken(@Value("${attendance.qr-secret:}") String secret) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  /** What a scan of a code says: the event, and whether the code is still fresh. */
  public record Verified(EventId eventId, boolean fresh) {}

  public String issue(EventId eventId, Instant now) {
    var slot = slotOf(now);
    return ENCODER.encodeToString(bytesOf(eventId.value()))
        + "."
        + slot
        + "."
        + ENCODER.encodeToString(mac(eventId, slot));
  }

  /** When the code made at {@code now} should be replaced by a new one. */
  public Instant refreshAt(Instant now) {
    return Instant.ofEpochSecond((slotOf(now) + 1) * SLOT.toSeconds());
  }

  /** Empty when the code was not made by us (altered, or for nothing). */
  public Optional<Verified> verify(String token, Instant now) {
    if (token == null) {
      return Optional.empty();
    }
    var parts = token.split("\\.", -1);
    if (parts.length != 3) {
      return Optional.empty();
    }
    try {
      var idBytes = DECODER.decode(parts[0]);
      if (idBytes.length != 16) {
        return Optional.empty();
      }
      var buffer = ByteBuffer.wrap(idBytes);
      var eventId = new EventId(new UUID(buffer.getLong(), buffer.getLong()));
      var slot = Long.parseLong(parts[1]);
      if (!MessageDigest.isEqual(DECODER.decode(parts[2]), mac(eventId, slot))) {
        return Optional.empty();
      }
      var current = slotOf(now);
      return Optional.of(new Verified(eventId, slot <= current && slot >= current - 1));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static long slotOf(Instant now) {
    return now.getEpochSecond() / SLOT.toSeconds();
  }

  private byte[] mac(EventId eventId, long slot) {
    if (secret.length == 0) {
      throw new IllegalStateException("attendance.qr-secret is not configured");
    }
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      mac.update(bytesOf(eventId.value()));
      mac.update(Long.toString(slot).getBytes(StandardCharsets.UTF_8));
      return Arrays.copyOf(mac.doFinal(), MAC_BYTES);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static byte[] bytesOf(UUID id) {
    return ByteBuffer.allocate(16)
        .putLong(id.getMostSignificantBits())
        .putLong(id.getLeastSignificantBits())
        .array();
  }
}
