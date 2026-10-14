package org.ouracademy.srtomy.event;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.ouracademy.srtomy.domain.DomainObject;
import org.ouracademy.srtomy.organization.Organization;
import org.ouracademy.srtomy.person.Person;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER, name = "event_type_id")
public class Event extends DomainObject {

  @Column(name = "name")
  String title;

  @OneToMany // opcional (ver 1 y 2)
  List<Person> speakers = new ArrayList<>();

  @Column(name = "start_time")
  ZonedDateTime start;

  @Column(name = "end_time")
  ZonedDateTime end;

  String address;

  @ManyToOne(optional = false)
  Organization organization;

  String description;

  @Column(name = "is_a_course")
  Boolean isACourse = false;

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

  public Event() {
    // jpa only
  }

  public Duration duration() {
    return Duration.between(start, end);
  }
}
