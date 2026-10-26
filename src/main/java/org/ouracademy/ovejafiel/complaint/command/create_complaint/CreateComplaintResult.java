package org.ouracademy.ovejafiel.complaint.command.create_complaint;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.complaint.domain.ComplaintId;

/**
 * The saved complaint. Carries every field (not only the id) because the endpoint has always
 * answered with the whole complaint, audit timestamps included.
 */
public record CreateComplaintResult(
    ComplaintId id,
    String firstName,
    String lastName,
    String dni,
    String address,
    String email,
    String comment,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {}
