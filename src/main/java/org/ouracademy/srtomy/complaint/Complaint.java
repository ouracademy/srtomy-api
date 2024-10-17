package org.ouracademy.srtomy.complaint;

import org.ouracademy.srtomy.domain.DomainObject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Complaint extends DomainObject {
    @NotBlank
    String firstName;
    @NotBlank
    String lastName;
    @Pattern(regexp = "^\\d{8}$")
    String dni;
    String address;
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Column(columnDefinition = "text")
    String comment;
}
