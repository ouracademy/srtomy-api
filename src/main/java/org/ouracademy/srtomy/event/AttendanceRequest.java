package org.ouracademy.srtomy.event;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AttendanceRequest {
  @NotNull List<Long> personIds = new ArrayList<>();
}
