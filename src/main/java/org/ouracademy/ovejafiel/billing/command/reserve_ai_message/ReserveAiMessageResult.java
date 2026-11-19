package org.ouracademy.ovejafiel.billing.command.reserve_ai_message;

/**
 * @param reason null when allowed; otherwise {@code quota} (the messages of the month) or {@code
 *     spend} (the estimated cost of the month)
 * @param message what to tell the person when it is not allowed, in Spanish
 * @param used messages of the month, this one included when allowed
 */
public record ReserveAiMessageResult(
    boolean allowed, String reason, int used, int limit, int remaining, String message) {

  public static final String QUOTA = "quota";
  public static final String SPEND = "spend";
}
