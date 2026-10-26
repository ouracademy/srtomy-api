package org.ouracademy.srtomy.notification.command.send_email;

/** What an outbox message of type {@link #TYPE} carries (as JSON). */
public record EmailPayload(String to, String subject, String text) {

  public static final String TYPE = "EMAIL";
}
