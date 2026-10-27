package org.ouracademy.ovejafiel.person.command.fill_person_email;

/** {@code filled} is false when the person already had an email, which is left as it was. */
public record FillPersonEmailResult(boolean filled) {}
