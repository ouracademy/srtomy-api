package org.ouracademy.srtomy.complaint.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.complaint.domain.Complaint;
import org.ouracademy.srtomy.complaint.repository.ComplaintRepository;

@ExtendWith(MockitoExtension.class)
class CreateComplaintCommandHandlerImplTests {

  @Mock ComplaintRepository complaintRepository;

  @Test
  void savesTheComplaintAndReturnsItsValues() {
    when(complaintRepository.save(any(Complaint.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var handler = new CreateComplaintCommandHandlerImpl(complaintRepository);

    var result =
        handler.handle(
            new CreateComplaintCommand(
                "Juan",
                "Perez",
                "12345678",
                "Av. Siempre Viva 742",
                "juan@example.com",
                "Everything was great"));

    var captor = ArgumentCaptor.forClass(Complaint.class);
    verify(complaintRepository).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.getFirstName()).isEqualTo("Juan");
    assertThat(saved.getLastName()).isEqualTo("Perez");
    assertThat(saved.getDni()).isEqualTo("12345678");
    assertThat(saved.getAddress()).isEqualTo("Av. Siempre Viva 742");
    assertThat(saved.getEmail()).isEqualTo("juan@example.com");
    assertThat(saved.getComment()).isEqualTo("Everything was great");

    assertThat(result.id()).isNotNull().isEqualTo(saved.getId());
    assertThat(result.firstName()).isEqualTo("Juan");
    assertThat(result.lastName()).isEqualTo("Perez");
    assertThat(result.dni()).isEqualTo("12345678");
    assertThat(result.address()).isEqualTo("Av. Siempre Viva 742");
    assertThat(result.email()).isEqualTo("juan@example.com");
    assertThat(result.comment()).isEqualTo("Everything was great");
  }

  @Test
  void dniAndAddressAreOptional() {
    when(complaintRepository.save(any(Complaint.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var handler = new CreateComplaintCommandHandlerImpl(complaintRepository);

    var result =
        handler.handle(
            new CreateComplaintCommand(
                "Juan", "Perez", null, null, "juan@example.com", "Everything was great"));

    assertThat(result.id()).isNotNull();
    assertThat(result.dni()).isNull();
    assertThat(result.address()).isNull();
  }
}
