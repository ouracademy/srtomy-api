package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.shared.domain.RandomSlugs;

@Entity
@Getter
// Hides deleted rows from every query, subclasses included. Rows are never removed: see delete().
@SQLRestriction("deleted_time is null")
public class Event extends DomainObject {

  @Id EventId id = EventId.generate();

  @Column(name = "name")
  String title;

  @Setter @OneToMany // opcional (ver 1 y 2)
  List<Person> speakers = new ArrayList<>();

  @Column(name = "start_time")
  ZonedDateTime start;

  @Column(name = "end_time")
  ZonedDateTime end;

  @Setter String address;

  @Setter
  @ManyToOne(optional = false)
  Organization organization;

  @Setter String description;

  LocalDateTime deletedTime;

  @Setter
  @Enumerated(EnumType.STRING)
  GeneralAccess generalAccess = GeneralAccess.RESTRICTED;

  @Setter
  @Enumerated(EnumType.STRING)
  LinkRole linkRole = LinkRole.VIEWER;

  /** Address of the shared page ({@code /e/{slug}}); a new one revokes the old link. */
  @Column(nullable = false)
  String shareSlug = RandomSlugs.next();

  @Setter
  @Column(name = "is_a_course")
  Boolean isACourse = false;

  /** The series this event is an occurrence of; null for a one-off event. */
  @ManyToOne EventSeries series;

  protected Event() {
    // jpa only
  }

  public Event(String title, ZonedDateTime start, ZonedDateTime end) {
    this.title = title;
    this.start = start;
    this.end = end;
  }

  // 1 { name: "Viernes con Jesús" }, 24 de noviembre, 7:30pm, jose mena
  // 2 { name: "Reunión de hombres" }, 28 de octubre, 7:30pm
  // 3 { name: "Celebremos juntos el dia del niño" }, domingo 20 agosto 10am
  // 4 { name: "Matrimonios Jovenes" }, // 11 de diciembre, no hora?
  // 5 { name: "Graduación grupo de mujeres" },

  // recurrent events
  // 6 { name: "Servicio Dominical" }, // domingos, 10:00 am
  // 7 { name: "Mujeres virtuosas" }, // miercoles a las 9:30am
  // 8 { name: "Servicio Dominical" }, // domingos, 10:00 am y 8pm
  // 9 { name: "Servicios" }, // domingos, 10:00 am y 8pm. miercoles a las 7pm

  // complex / composite events
  // 10 { name: "Sobredosis Garden" }, // jueves 27-30 de agosto (ver:
  // https://www.facebook.com/photo/?fbid=673593008144144&set=pcb.673593201477458)

  /**
   * Soft delete. Not {@code em.remove()}: a removed entity is still referenced by its managed
   * attendance, which makes the flush fail, and the attendance cascade would remove its rows too.
   */
  public void delete() {
    this.deletedTime = LocalDateTime.now();
  }

  /**
   * Changes who can open the event through its link. Signing up through the link is only for
   * courses.
   */
  public void shareVia(GeneralAccess generalAccess, LinkRole linkRole) {
    if (linkRole == LinkRole.REGISTRANT && !Boolean.TRUE.equals(isACourse)) {
      throw new DomainValidationException("Solo se puede abrir la inscripción de un curso");
    }
    this.generalAccess = generalAccess;
    this.linkRole = linkRole;
  }

  /** Revokes the current link: the old address stops working. */
  public void renewShareLink() {
    this.shareSlug = RandomSlugs.next();
  }

  public Duration duration() {
    return Duration.between(start, end);
  }
}
