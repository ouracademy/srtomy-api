package org.ouracademy.ovejafiel.complaint.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintCommand;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintCommandHandler;
import org.ouracademy.ovejafiel.complaint.command.create_complaint.CreateComplaintResult;
import org.ouracademy.ovejafiel.complaint.domain.ComplaintId;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ComplaintController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class ComplaintControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean CreateComplaintCommandHandler createComplaintCommandHandler;

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  private void givenTheHandlerSavesWith(ComplaintId id, LocalDateTime createdAt) {
    given(createComplaintCommandHandler.handle(any(CreateComplaintCommand.class)))
        .willAnswer(
            invocation -> {
              CreateComplaintCommand command = invocation.getArgument(0);
              return new CreateComplaintResult(
                  id,
                  command.firstName(),
                  command.lastName(),
                  command.dni(),
                  command.address(),
                  command.email(),
                  command.comment(),
                  createdAt,
                  createdAt);
            });
  }

  @Test
  void createWithAllRequiredFieldsReturns201() {
    var id = ComplaintId.generate();
    givenTheHandlerSavesWith(id, LocalDateTime.of(2026, 10, 21, 10, 30));

    var requestBody =
        """
        {
          "firstName": "Juan",
          "lastName": "Perez",
          "dni": "12345678",
          "email": "juan@example.com",
          "comment": "Everything was great"
        }
        """;

    var result =
        assertThat(
                mvc.post()
                    .uri("/v1/complaints")
                    .header("Authorization", bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .hasStatus(201)
            .bodyJson();
    result.extractingPath("$.id").isEqualTo(id.value().toString());
    result.extractingPath("$.firstName").isEqualTo("Juan");
    result.extractingPath("$.lastName").isEqualTo("Perez");
    result.extractingPath("$.dni").isEqualTo("12345678");
    result.extractingPath("$.email").isEqualTo("juan@example.com");
    result.extractingPath("$.comment").isEqualTo("Everything was great");
    result.extractingPath("$.createDate").isEqualTo("2026-10-21T10:30:00");
    result.extractingPath("$.lastModifiedDate").isEqualTo("2026-10-21T10:30:00");

    verify(createComplaintCommandHandler)
        .handle(
            new CreateComplaintCommand(
                "Juan", "Perez", "12345678", null, "juan@example.com", "Everything was great"));
  }

  @Test
  void createWithoutRequiredFieldsIsABadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/complaints")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(400);
    verifyNoInteractions(createComplaintCommandHandler);
  }

  @Test
  void createWithoutDniReturns201() {
    // dni is optional: not every complaint comes with an identity document
    // (e.g. anonymous complaints).
    givenTheHandlerSavesWith(ComplaintId.generate(), LocalDateTime.now());

    var requestBody =
        """
        {
          "firstName": "Juan",
          "lastName": "Perez",
          "email": "juan@example.com",
          "comment": "Everything was great"
        }
        """;

    assertThat(
            mvc.post()
                .uri("/v1/complaints")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .hasStatus(201);
  }

  @Test
  void createWithAnInvalidDniIsABadRequest() {
    var requestBody =
        """
        {
          "firstName": "Juan",
          "lastName": "Perez",
          "dni": "not-a-dni",
          "email": "juan@example.com",
          "comment": "Everything was great"
        }
        """;

    assertThat(
            mvc.post()
                .uri("/v1/complaints")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .hasStatus(400);
    verifyNoInteractions(createComplaintCommandHandler);
  }

  @Test
  void createWithAnInvalidEmailIsABadRequest() {
    var requestBody =
        """
        {
          "firstName": "Juan",
          "lastName": "Perez",
          "email": "not-an-email",
          "comment": "Everything was great"
        }
        """;

    assertThat(
            mvc.post()
                .uri("/v1/complaints")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .hasStatus(400);
    verifyNoInteractions(createComplaintCommandHandler);
  }
}
