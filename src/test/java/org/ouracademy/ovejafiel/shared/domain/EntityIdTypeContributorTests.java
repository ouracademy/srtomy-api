package org.ouracademy.ovejafiel.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.complaint.domain.ComplaintId;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetupId;
import org.ouracademy.ovejafiel.event.domain.EventSeriesId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.domain.UserPersonLinkId;

class EntityIdTypeContributorTests {

  @Test
  void registersAJavaTypeForEveryEntityIdRecord() {
    var typeContributions = mock(TypeContributions.class);
    var typeConfiguration = mock(TypeConfiguration.class);
    var javaTypeRegistry = mock(JavaTypeRegistry.class);
    when(typeContributions.getTypeConfiguration()).thenReturn(typeConfiguration);
    when(typeConfiguration.getJavaTypeRegistry()).thenReturn(javaTypeRegistry);

    new EntityIdTypeContributor().contribute(typeContributions, mock(ServiceRegistry.class));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<JavaType<?>> captor = ArgumentCaptor.forClass(JavaType.class);
    verify(javaTypeRegistry, times(16)).addDescriptor(captor.capture());

    assertThat(captor.getAllValues())
        .<Class<?>>extracting(JavaType::getJavaTypeClass)
        .containsExactlyInAnyOrder(
            PartyId.class,
            InvitationId.class,
            EventShareId.class,
            EventRegistrationId.class,
            EventRegistrationSetupId.class,
            TicketTypeId.class,
            EventId.class,
            EventSeriesId.class,
            AccountabilityId.class,
            AccountabilityTypeId.class,
            ComplaintId.class,
            UserId.class,
            UserPersonLinkId.class,
            IamRoleId.class,
            RoleBindingId.class,
            OutboxMessageId.class);
  }
}
