package org.ouracademy.ovejafiel.person.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.person.domain.MemberState;

@Getter
@Setter
public class MembershipRequest {

  /** How involved the person is in this node: visitor, membership in progress or member. */
  @NotNull MemberState memberState;
}
