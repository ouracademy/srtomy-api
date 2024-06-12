package org.ouracademy.srtomy.event;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceRequest {
    @NotNull
    List<Long> personIds = new ArrayList<>();
}
