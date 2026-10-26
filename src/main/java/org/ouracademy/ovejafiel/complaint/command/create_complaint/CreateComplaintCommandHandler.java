package org.ouracademy.ovejafiel.complaint.command.create_complaint;

public interface CreateComplaintCommandHandler {
  CreateComplaintResult handle(CreateComplaintCommand command);
}
