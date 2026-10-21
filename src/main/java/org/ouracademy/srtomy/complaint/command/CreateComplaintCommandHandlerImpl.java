package org.ouracademy.srtomy.complaint.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.complaint.domain.Complaint;
import org.ouracademy.srtomy.complaint.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateComplaintCommandHandlerImpl implements CreateComplaintCommandHandler {

  private final ComplaintRepository complaintRepository;

  @Override
  @Transactional
  public CreateComplaintResult handle(CreateComplaintCommand command) {
    var complaint = new Complaint();
    complaint.setFirstName(command.firstName());
    complaint.setLastName(command.lastName());
    complaint.setDni(command.dni());
    complaint.setAddress(command.address());
    complaint.setEmail(command.email());
    complaint.setComment(command.comment());

    var saved = complaintRepository.save(complaint);
    return new CreateComplaintResult(
        saved.getId(),
        saved.getFirstName(),
        saved.getLastName(),
        saved.getDni(),
        saved.getAddress(),
        saved.getEmail(),
        saved.getComment(),
        saved.getCreateDate(),
        saved.getLastModifiedDate());
  }
}
