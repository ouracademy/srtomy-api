package org.ouracademy.ovejafiel.whatsapp.domain;

/** What WhatsApp needs from a phone: the whole number, country code included. */
public final class PhoneNumbers {

  private static final int MIN_DIGITS = 8;

  private PhoneNumbers() {}

  /**
   * Whether the phone was written with its country code ({@code +51 999 888 777}). A number without
   * it ({@code 999 888 777}) is ambiguous: the same digits belong to different countries, and Meta
   * refuses to deliver to it. The forms of the portal add the code; older records may lack it.
   */
  public static boolean isInternational(String phone) {
    if (phone == null || !phone.strip().startsWith("+")) {
      return false;
    }
    return LinkCodes.digitsOf(phone).length() >= MIN_DIGITS;
  }
}
