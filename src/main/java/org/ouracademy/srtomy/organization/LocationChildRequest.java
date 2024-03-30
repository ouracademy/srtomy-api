package org.ouracademy.srtomy.organization;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LocationChildRequest extends LocationRequest {

    @NotNull
    Organization.Type type;

    public boolean isChild() {
        return type.equals(Organization.Type.GROUP) || type.equals(Organization.Type.MINISTRY);
    }
}
