package org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp;

public record LinkWhatsAppResult(Status status, String churchName) {

  public enum Status {
    /** The number is now proven: the next messages from it act as the account. */
    LINKED,
    /** The code is wrong, expired or already used. */
    INVALID_CODE,
    /** Too many wrong codes from that number lately: even the right one is refused for a while. */
    RATE_LIMITED,
    CHURCH_NOT_CONNECTED
  }

  static LinkWhatsAppResult of(Status status) {
    return new LinkWhatsAppResult(status, null);
  }
}
