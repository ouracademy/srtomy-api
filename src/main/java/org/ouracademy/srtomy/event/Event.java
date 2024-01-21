package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.ouracademy.srtomy.domain.DomainObject;
import org.ouracademy.srtomy.organization.Organization;
import org.ouracademy.srtomy.person.Person;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;

@Entity
@Getter
public class Event extends DomainObject implements EventSimpleItem {

    String name;

    @OneToMany // opcional (ver 1 y 2)
    List<Person> speakers = new ArrayList<>();

    // opcional en caso que solo quiera registrar asistencia (ver 5)
    ZonedDateTime startTime;
    // opcional? (ver 1, 2 y 3)
    ZonedDateTime endTime;

    String address;

    @ManyToOne(optional = false)
    Organization organization;

    String description;

    // 1 { name: "Viernes con Jesús" }, 24 de noviembre, 7:30pm, jose mena
    // 2 { name: "Reunión de hombres" }, 28 de octubre, 7:30pm
    // 3 { name: "Celebremos juntos el dia del niño" }, domingo 20 agosto 10am
    // 4 { name: "Matrimonios Jovenes" }, // 11 de diciembre, no hora?

    // 5 { name: "Graduación grupo de mujeres" },

    // TODO: lalalala
    // 6 { name: "Servicio Dominical" }, // domingos, 10:00 am
    // 7 { name: "Mujeres virtuosas" }, // miercoles a las 9:30am
    // 8 { name: "Servicio Dominical" }, // domingos, 10:00 am y 8pm

    // 9 { name: "Servicios" }, // domingos, 10:00 am y 8pm. miercoles a las 7pm

    // TODO: para casos complejos?
    // 10 { name: "Sobredosis Garden" }, // jueves 27-30 de agosto (ver:
    // https://www.facebook.com/photo/?fbid=673593008144144&set=pcb.673593201477458)

    public Event() {
        // jpa only
    }

    public Event(String name, ZonedDateTime startTime, ZonedDateTime endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

}
