package org.ouracademy.srtomy.person;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePersonRequest extends UpdatePersonRequest {

    @NotNull
    Long organizationId;
}
