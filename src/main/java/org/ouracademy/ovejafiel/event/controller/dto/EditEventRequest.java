package org.ouracademy.ovejafiel.event.controller.dto;

import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;

@Getter
@Setter
public class EditEventRequest extends EventRequest {
  /** For a recurring event, which occurrences change; only this one when omitted. */
  EventChangeOption option;
}
