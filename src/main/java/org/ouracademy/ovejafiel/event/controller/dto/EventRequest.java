package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class EventRequest {
  @NotBlank String name;

  @NotNull String startTime;

  @NotNull String endTime;

  List<UUID> speakerIds = new ArrayList<>();

  String address;

  @NotNull UUID organizationId;

  String description;

  Boolean isACourse = false;

  List<Integer> daysOfWeek = new ArrayList<>();

  LocalDate endRecur;

  @AssertTrue
  public boolean isValidDaysOfWeek() {
    if (isRecurrent()) {
      return !daysOfWeek.isEmpty();
    } else {
      return true;
    }
  }

  public boolean isRecurrent() {
    return endRecur != null;
  }
}
