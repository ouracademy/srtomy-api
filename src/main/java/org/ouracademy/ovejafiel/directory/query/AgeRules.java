package org.ouracademy.ovejafiel.directory.query;

import java.time.LocalDate;
import java.time.Period;

/**
 * Who may be listed by name. Minors are never listed in a public page. Inside the church, a person
 * whose age is not known is listed (the church sees its own people), but a public page lists only
 * people known to be 18 or older.
 */
public final class AgeRules {

  public static final int ADULT_AGE = 18;

  private AgeRules() {}

  /** True when the data says the person is under 18; false when adult or unknown. */
  public static boolean isKnownMinor(Integer age, LocalDate birthdate, LocalDate today) {
    var years = years(age, birthdate, today);
    return years != null && years < ADULT_AGE;
  }

  /** True only when the data says the person is 18 or older. */
  public static boolean isKnownAdult(Integer age, LocalDate birthdate, LocalDate today) {
    var years = years(age, birthdate, today);
    return years != null && years >= ADULT_AGE;
  }

  // The birthdate wins over the stored age, which can be out of date.
  private static Integer years(Integer age, LocalDate birthdate, LocalDate today) {
    if (birthdate != null) {
      return Period.between(birthdate, today).getYears();
    }
    return age;
  }
}
