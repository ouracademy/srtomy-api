package org.ouracademy.srtomy.shared.domain;

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
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.complaint.ComplaintId;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.party.domain.PartyId;

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
    verify(javaTypeRegistry, times(5)).addDescriptor(captor.capture());

    assertThat(captor.getAllValues())
        .<Class<?>>extracting(JavaType::getJavaTypeClass)
        .containsExactlyInAnyOrder(
            PartyId.class,
            EventId.class,
            AccountabilityId.class,
            AccountabilityTypeId.class,
            ComplaintId.class);
  }
}
