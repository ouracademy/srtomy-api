package org.ouracademy.ovejafiel.event.controller.dto;

import org.ouracademy.ovejafiel.event.domain.EventChangeOption;

/** Which events of a recurring series to delete; only this one when not said. */
public record EventDeleteRequest(EventChangeOption option) {

  public EventDeleteRequest {
    option = option == null ? EventChangeOption.THIS_EVENT : option;
  }
}
