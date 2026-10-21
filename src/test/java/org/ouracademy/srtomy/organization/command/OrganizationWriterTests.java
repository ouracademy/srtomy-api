package org.ouracademy.srtomy.organization.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilityCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class OrganizationWriterTests {

  static final OrganizationInfo INFO = new OrganizationInfo("Name", "desc", "addr", "123");

  @Mock OrganizationRepository organizationRepository;
  @Mock CreateAccountabilityCommandHandler createAccountabilityCommandHandler;

  OrganizationWriter writer;

  @BeforeEach
  void setUp() {
    writer = new OrganizationWriter(organizationRepository, createAccountabilityCommandHandler);
  }

  @Test
  void createWithAParentLinksTheNewOrganizationToItAsIsPartOf() {
    var parentId = PartyId.generate();
    when(organizationRepository.save(any(Organization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var created = writer.create(INFO, Organization.Type.LOCATION, parentId);

    assertThat(created.getName()).isEqualTo("Name");
    assertThat(created.getDescription()).isEqualTo("desc");
    assertThat(created.getAddress()).isEqualTo("addr");
    assertThat(created.getPhoneNumber()).isEqualTo("123");
    assertThat(created.getType()).isEqualTo(Organization.Type.LOCATION);
    verify(createAccountabilityCommandHandler)
        .handle(new CreateAccountabilityCommand(created.getId(), parentId, IS_PART_OF));
  }

  @Test
  void createWithoutAParentCreatesNoAccountability() {
    when(organizationRepository.save(any(Organization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    writer.create(INFO, Organization.Type.CHURCH, null);

    verifyNoInteractions(createAccountabilityCommandHandler);
  }

  @Test
  void editInfoOverwritesTheFieldsOrIsEmptyWhenTheOrganizationDoesNotExist() {
    var id = PartyId.generate();
    var existing = organization(id, "Old", Organization.Type.GROUP);
    when(organizationRepository.findById(id)).thenReturn(Optional.of(existing));
    when(organizationRepository.save(existing)).thenReturn(existing);
    var missing = PartyId.generate();
    when(organizationRepository.findById(missing)).thenReturn(Optional.empty());

    assertThat(writer.editInfo(id, INFO)).get().extracting(Organization::getName).isEqualTo("Name");
    assertThat(existing.getAddress()).isEqualTo("addr");
    assertThat(writer.editInfo(missing, INFO)).isEmpty();
  }
}
