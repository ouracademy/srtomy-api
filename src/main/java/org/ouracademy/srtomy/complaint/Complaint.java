package org.ouracademy.srtomy.complaint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.domain.DomainObject;

@Entity
@Getter
@Setter
public class Complaint extends DomainObject {
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
