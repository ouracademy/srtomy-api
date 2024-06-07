package org.ouracademy.srtomy.person;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePersonRequest extends CreatePersonRequest {

    @NotEmpty
    @Override
    public Set<Long> getRoles() {
        return super.getRoles();
    }

    @Override
    public void setRoles(Set<Long> roles) {
        super.setRoles(roles);
    }
}
