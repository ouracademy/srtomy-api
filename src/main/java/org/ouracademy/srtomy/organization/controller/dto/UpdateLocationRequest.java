package org.ouracademy.srtomy.organization.controller.dto;

import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;

public class UpdateLocationRequest extends OrganizationRequest {

  @Getter @Setter SortedSet<String> roles = new TreeSet<>();
}
