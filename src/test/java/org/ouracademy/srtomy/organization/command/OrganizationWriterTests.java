package org.ouracademy.srtomy.organization.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.domain.SocialLinks;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

@ExtendWith(MockitoExtension.class)
class OrganizationWriterTests {

  static final SocialLinks LINKS =
      new SocialLinks("https://x.org", "https://facebook.com/x", null, null, null);
  static final OrganizationInfo INFO = new OrganizationInfo("Name", "desc", "addr", "123", LINKS);

  @Mock OrganizationRepository organizationRepository;

  OrganizationWriter writer;

  @BeforeEach
  void setUp() {
    writer = new OrganizationWriter(organizationRepository);
  }

  @Test
  void createUnderAParentSetsTheParentAndTheInfo() {
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    when(organizationRepository.findById(church.getId())).thenReturn(Optional.of(church));
    when(organizationRepository.save(any(Organization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var created = writer.create(INFO, Organization.Type.LOCATION, church.getId());

    assertThat(created.getName()).isEqualTo("Name");
    assertThat(created.getDescription()).isEqualTo("desc");
    assertThat(created.getAddress()).isEqualTo("addr");
    assertThat(created.getPhoneNumber()).isEqualTo("123");
    assertThat(created.getSocialLinks()).isEqualTo(LINKS);
    assertThat(created.getType()).isEqualTo(Organization.Type.LOCATION);
    assertThat(created.getParent()).isSameAs(church);
  }

  @Test
  void createWithoutAParentCreatesARootChurch() {
    when(organizationRepository.save(any(Organization.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var created = writer.create(INFO, Organization.Type.CHURCH, null);

    assertThat(created.getParent()).isNull();
  }

  @Test
  void createRejectsATypeThatCannotHangFromTheParent() {
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    when(organizationRepository.findById(church.getId())).thenReturn(Optional.of(church));

    assertThatThrownBy(() -> writer.create(INFO, Organization.Type.GROUP, church.getId()))
        .isInstanceOf(DomainValidationException.class);
    verify(organizationRepository, never()).save(any(Organization.class));
  }

  @Test
  void createFailsWhenTheParentDoesNotExist() {
    var missing = PartyId.generate();
    when(organizationRepository.findById(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> writer.create(INFO, Organization.Type.LOCATION, missing))
        .isInstanceOf(NoSuchElementException.class);
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

  @Test
  void editChildUpdatesTheInfoAndTypeOfAGroup() {
    var id = PartyId.generate();
    var existing = organization(id, "Old", Organization.Type.GROUP);
    when(organizationRepository.findById(id)).thenReturn(Optional.of(existing));
    when(organizationRepository.save(existing)).thenReturn(existing);

    var edited = writer.editChild(id, INFO, Organization.Type.MINISTRY);

    assertThat(edited).get().extracting(Organization::getName).isEqualTo("Name");
    assertThat(existing.getType()).isEqualTo(Organization.Type.MINISTRY);
    assertThat(existing.getDescription()).isEqualTo("desc");
    assertThat(existing.getAddress()).isEqualTo("addr");
    assertThat(existing.getPhoneNumber()).isEqualTo("123");
  }

  @Test
  void editChildIsEmptyAndSavesNothingWhenTheOrganizationIsNotAGroupOrMinistry() {
    var id = PartyId.generate();
    var church = organization(id, "Church", Organization.Type.CHURCH);
    when(organizationRepository.findById(id)).thenReturn(Optional.of(church));

    assertThat(writer.editChild(id, INFO, Organization.Type.GROUP)).isEmpty();
    assertThat(church.getName()).isEqualTo("Church");
    verify(organizationRepository, never()).save(any(Organization.class));
  }

  @Test
  void editChildIsEmptyWhenTheOrganizationDoesNotExist() {
    var missing = PartyId.generate();
    when(organizationRepository.findById(missing)).thenReturn(Optional.empty());

    assertThat(writer.editChild(missing, INFO, Organization.Type.GROUP)).isEmpty();
    verify(organizationRepository, never()).save(any(Organization.class));
  }
}
