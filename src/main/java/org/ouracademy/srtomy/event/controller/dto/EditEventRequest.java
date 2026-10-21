package org.ouracademy.srtomy.event.controller.dto;

import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.event.domain.EventChangeOption;

@Getter
@Setter
public class EditEventRequest extends EventRequest {
  EventChangeOption option;
}
