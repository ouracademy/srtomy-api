package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRequest {
    @NotBlank
    String name;

    @NotNull
    LocalDateTime startTime;
    @NotNull
    LocalDateTime endTime;

    List<Long> speakerIds = new ArrayList<>();

    String address;

    @NotNull
    Long organizationId;
}
