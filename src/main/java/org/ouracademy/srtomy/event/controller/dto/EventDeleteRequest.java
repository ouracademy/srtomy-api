package org.ouracademy.srtomy.event.controller.dto;

import lombok.Data;
import org.ouracademy.srtomy.event.domain.EventChangeOption;

@Data
public class EventDeleteRequest {
  EventChangeOption option = EventChangeOption.THIS_EVENT;
}
