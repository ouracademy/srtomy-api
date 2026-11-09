package org.ouracademy.ovejafiel.person.domain;

import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.party.domain.Party;

@Entity
@Getter
@Setter
public class Person extends Party {

  String firstName;
  String lastName;

  String email;

  Integer age;
  LocalDate birthdate;
  Boolean hasBeenBaptized;

  /** Accepts WhatsApp notices from the church; on until the person asks to stop. */
  boolean whatsappConsent = true;

  /** When the person was removed from the church; null while they belong to it. */
  LocalDateTime deletedTime;

  public void delete() {
    this.deletedTime = LocalDateTime.now();
  }

  public void restore() {
    this.deletedTime = null;
  }

  public String getFullName() {
    return name;
  }
}
