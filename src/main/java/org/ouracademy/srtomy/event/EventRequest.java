package org.ouracademy.srtomy.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRequest {
    @NotBlank
    String name;

    @NotNull
    String startTime;

    @NotNull
    String endTime;

    List<Long> speakerIds = new ArrayList<>();

    String address;

    @NotNull
    Long organizationId;

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

    Boolean isRecurrent() {
        return endRecur != null;
    }
}
