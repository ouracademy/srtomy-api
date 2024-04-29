package org.ouracademy.srtomy.organization;

import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

public class UpdateLocationRequest extends OrganizationRequest {

    @Getter
    @Setter
    @NotEmpty
    SortedSet<String> roles = new TreeSet<>();
}
