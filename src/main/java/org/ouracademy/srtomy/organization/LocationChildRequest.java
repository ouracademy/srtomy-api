package org.ouracademy.srtomy.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LocationChildRequest extends LocationRequest {

    @NotNull
    Organization.Type type;

    @Schema(hidden = true)
    @AssertTrue
    public boolean isChild() {
        return type.equals(Organization.Type.GROUP) || type.equals(Organization.Type.MINISTRY);
    }
}
