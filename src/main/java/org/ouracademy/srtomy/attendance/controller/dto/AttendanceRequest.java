package org.ouracademy.srtomy.attendance.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class AttendanceRequest {
  @NotNull List<UUID> personIds = new ArrayList<>();
}
