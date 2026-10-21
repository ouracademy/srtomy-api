package org.ouracademy.srtomy.complaint.command;

public interface CreateComplaintCommandHandler {
  CreateComplaintResult handle(CreateComplaintCommand command);
}
