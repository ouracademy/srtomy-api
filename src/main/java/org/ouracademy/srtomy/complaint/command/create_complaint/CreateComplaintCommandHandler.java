package org.ouracademy.srtomy.complaint.command.create_complaint;

public interface CreateComplaintCommandHandler {
  CreateComplaintResult handle(CreateComplaintCommand command);
}
