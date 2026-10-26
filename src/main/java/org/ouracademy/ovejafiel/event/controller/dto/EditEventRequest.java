package org.ouracademy.ovejafiel.event.controller.dto;

import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;

@Getter
@Setter
public class EditEventRequest extends EventRequest {
  EventChangeOption option;
}
