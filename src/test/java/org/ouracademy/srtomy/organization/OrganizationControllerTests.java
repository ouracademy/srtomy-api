package org.ouracademy.srtomy.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.SecurityConfig;
import org.ouracademy.srtomy.TestSecurityConfig;
import org.ouracademy.srtomy.auth0.Auth0Gateway;
import org.ouracademy.srtomy.person.AccountabilityRepository;
import org.ouracademy.srtomy.person.AccountabilityType;
import org.ouracademy.srtomy.person.AccountabilityType.Category;
import org.ouracademy.srtomy.person.AccountabilityTypeRepository;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OrganizationController.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class OrganizationControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean OrganizationRepository organizationRepository;
  @MockitoBean AccountabilityRepository accountabilityRepository;
  @MockitoBean PersonRepository personRepository;
  @MockitoBean RoleService roleService;
  @MockitoBean AccountabilityTypeRepository accountabilityTypeRepository;
  @MockitoBean Auth0Gateway auth0Gateway;

  @Test
  void predefinedRolesReturnsTheSeededRoles() {
    given(roleService.getPredefinedRoles())
        .willReturn(
            List.of(
                new AccountabilityType("PASTOR", null, Category.ROLE),
                new AccountabilityType("WORSHIP_LEADER", null, Category.ROLE),
                new AccountabilityType("USHER", null, Category.ROLE)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/locations/predefined-roles")
                .header("Authorization", "Bearer " + TestSecurityConfig.validToken(jwtEncoder)))
        .hasStatusOk()
        .bodyText()
        .contains("PASTOR", "WORSHIP_LEADER", "USHER");
  }
}
