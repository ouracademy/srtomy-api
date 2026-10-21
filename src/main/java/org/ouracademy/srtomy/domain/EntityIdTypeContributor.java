package org.ouracademy.srtomy.domain;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.ouracademy.srtomy.complaint.ComplaintId;
import org.ouracademy.srtomy.event.EventId;
import org.ouracademy.srtomy.person.AccountabilityId;
import org.ouracademy.srtomy.person.AccountabilityTypeId;
import org.ouracademy.srtomy.person.PartyId;

/** Registers every {@link EntityId} record as a Hibernate basic type backed by a UUID column. */
public class EntityIdTypeContributor implements TypeContributor {

  @Override
  public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    var javaTypeRegistry = typeContributions.getTypeConfiguration().getJavaTypeRegistry();
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(PartyId.class, PartyId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(EventId.class, EventId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(AccountabilityId.class, AccountabilityId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(AccountabilityTypeId.class, AccountabilityTypeId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(ComplaintId.class, ComplaintId::new));
  }
}
