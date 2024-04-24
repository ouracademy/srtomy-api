package org.ouracademy.srtomy.organization;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class LocationRequest extends OrganizationRequest {

    @NotEmpty
    List<String> roles = new ArrayList<>();

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
