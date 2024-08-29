package org.ouracademy.srtomy.organization;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationRequest {
    @NotBlank
    String name;

    String description;

    String address;

    String phoneNumber;

    Long parentId;

    // TODO: remove this and use current user (principal)
    Long userId;
}
