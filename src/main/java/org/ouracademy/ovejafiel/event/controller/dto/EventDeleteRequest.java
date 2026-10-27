package org.ouracademy.ovejafiel.event.controller.dto;

import lombok.Data;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;

@Data
public class EventDeleteRequest {
  EventChangeOption option = EventChangeOption.THIS_EVENT;
}
