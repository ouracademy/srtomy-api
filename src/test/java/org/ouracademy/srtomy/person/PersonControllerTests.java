package org.ouracademy.srtomy.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.SecurityConfig;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.ouracademy.srtomy.organization.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PersonController.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class PersonControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean AccountabilityRepository accountabilityRepository;
  @MockitoBean AccountabilityTypeRepository accountabilityTypeRepository;
  @MockitoBean PersonRepository personRepository;
  @MockitoBean OrganizationRepository organizationRepository;
  @MockitoBean RoleService roleService;

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void findAllForAnUnknownOrganizationReturnsAnEmptyPage() {
    var organizationId = UUID.randomUUID();
    given(accountabilityTypeRepository.getReferenceById(SystemAccountabilityTypes.IS_PART_OF))
        .willReturn(new AccountabilityType());
    given(personRepository.findByOrgId(eq(new PartyId(organizationId)), any(), any()))
        .willReturn(Page.empty());

    assertThat(
            mvc.get()
                .uri("/v1/people?organizationId=" + organizationId)
                .header("Authorization", bearerHeader()))
        .hasStatusOk()
        .bodyText()
        .contains("\"totalElements\":0");
  }

  @Test
  void findAllWithoutOrganizationIdIsABadRequest() {
    assertThat(mvc.get().uri("/v1/people").header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }
}
