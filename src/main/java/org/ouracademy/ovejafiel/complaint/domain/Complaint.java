package org.ouracademy.ovejafiel.complaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

@Entity
@Getter
@Setter
public class Complaint extends DomainObject {

  @Id ComplaintId id = ComplaintId.generate();

  @NotBlank String firstName;
  @NotBlank String lastName;

  // Optional: not every complaint comes with an identity document (e.g. anonymous
  // complaints). When present, it must still look like a real DNI.
  @Pattern(regexp = "^\\d{8}$")
  String dni;

  String address;
  @NotBlank @Email String email;

  @NotBlank
  @Column(columnDefinition = "text")
  String comment;
}
