package org.ouracademy.srtomy.organization;

import jakarta.validation.constraints.NotNull;

public class LocationRequest extends OrganizationRequest {
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
