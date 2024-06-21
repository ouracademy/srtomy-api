package org.ouracademy.srtomy.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditEventRequest extends EventRequest {
    EventChangeOption option;
}
