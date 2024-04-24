package org.ouracademy.srtomy.organization;

import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class LocationRequest extends OrganizationRequest {

    @NotEmpty
    SortedSet<String> roles = new TreeSet<>();

    @NotNull
    @Override
    public Long getParentId() {
        return super.getParentId();
    }

    @Override
    public void setParentId(Long parentId) {
        super.setParentId(parentId);
    }
}
