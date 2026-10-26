package org.ouracademy.ovejafiel.complaint.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintResult;

/** Same JSON fields the endpoint returned when it serialized the Complaint entity. */
public record ComplaintResponse(
    UUID id,
    String firstName,
    String lastName,
    String dni,
    String address,
    String email,
    String comment,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static ComplaintResponse from(CreateComplaintResult result) {
    return new ComplaintResponse(
        result.id().value(),
        result.firstName(),
        result.lastName(),
        result.dni(),
        result.address(),
        result.email(),
        result.comment(),
        result.createDate(),
        result.lastModifiedDate());
  }
}
