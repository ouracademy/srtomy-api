package org.ouracademy.srtomy.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.command.SyncOrganizationRolesCommandHandler;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.query.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.query.GetRolesOfLocationQueryHandler;
import org.ouracademy.srtomy.accountability.query.GetRolesOfPersonQueryHandler;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.auth.gateway.Auth0Gateway;
import org.ouracademy.srtomy.person.PersonRepository;
import org.ouracademy.srtomy.shared.SecurityConfig;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OrganizationController.class)
@ContextConfiguration(classes = SrtomyApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class OrganizationControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean OrganizationRepository organizationRepository;
  @MockitoBean AccountabilityRepository accountabilityRepository;
  @MockitoBean PersonRepository personRepository;
  @MockitoBean SyncOrganizationRolesCommandHandler syncOrganizationRolesCommandHandler;
  @MockitoBean GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  @MockitoBean GetRolesOfLocationQueryHandler getRolesOfLocationQueryHandler;
  @MockitoBean GetRolesOfPersonQueryHandler getRolesOfPersonQueryHandler;
  @MockitoBean AccountabilityTypeRepository accountabilityTypeRepository;
  @MockitoBean Auth0Gateway auth0Gateway;

  @Test
  void predefinedRolesReturnsTheSeededRoles() {
    given(getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery()))
        .willReturn(
            List.of(
                new AccountabilityTypeResult(AccountabilityTypeId.generate(), "PASTOR", true),
                new AccountabilityTypeResult(
                    AccountabilityTypeId.generate(), "WORSHIP_LEADER", true),
                new AccountabilityTypeResult(AccountabilityTypeId.generate(), "USHER", true)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/locations/predefined-roles")
                .header("Authorization", "Bearer " + TestSecurityConfig.validToken(jwtEncoder)))
        .hasStatusOk()
        .bodyText()
        .contains("PASTOR", "WORSHIP_LEADER", "USHER");
  }
}
