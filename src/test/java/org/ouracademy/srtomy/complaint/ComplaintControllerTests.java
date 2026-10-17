package org.ouracademy.srtomy.complaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.SecurityConfig;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ComplaintController.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class ComplaintControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean ComplaintRepository complaintRepository;

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void createWithAllRequiredFieldsReturns201() {
    given(complaintRepository.save(any(Complaint.class)))
        .willAnswer(
            invocation -> {
              Complaint complaint = invocation.getArgument(0);
              ReflectionTestUtils.setField(complaint, "id", 1L);
              return complaint;
            });

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

    assertThat(
            mvc.post()
                .uri("/v1/complaints")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(1);
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
  }

  @Test
  void createWithoutDniReturns201() {
    // dni is optional: not every complaint comes with an identity document
    // (e.g. anonymous complaints).
    given(complaintRepository.save(any(Complaint.class)))
        .willAnswer(
            invocation -> {
              Complaint complaint = invocation.getArgument(0);
              ReflectionTestUtils.setField(complaint, "id", 2L);
              return complaint;
            });

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
  }
}
