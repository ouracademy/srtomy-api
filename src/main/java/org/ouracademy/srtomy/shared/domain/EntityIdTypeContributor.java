package org.ouracademy.srtomy.shared.domain;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.complaint.domain.ComplaintId;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventRegistrationId;
import org.ouracademy.srtomy.event.domain.EventSeriesId;
import org.ouracademy.srtomy.event.domain.EventShareId;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.domain.UserPersonLinkId;

/** Registers every {@link EntityId} record as a Hibernate basic type backed by a UUID column. */
public class EntityIdTypeContributor implements TypeContributor {

  @Override
  public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    var javaTypeRegistry = typeContributions.getTypeConfiguration().getJavaTypeRegistry();
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(PartyId.class, PartyId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(EventId.class, EventId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(EventSeriesId.class, EventSeriesId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(AccountabilityId.class, AccountabilityId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(AccountabilityTypeId.class, AccountabilityTypeId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(ComplaintId.class, ComplaintId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(UserId.class, UserId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(UserPersonLinkId.class, UserPersonLinkId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(IamRoleId.class, IamRoleId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(RoleBindingId.class, RoleBindingId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(InvitationId.class, InvitationId::new));
    javaTypeRegistry.addDescriptor(new EntityIdJavaType<>(EventShareId.class, EventShareId::new));
    javaTypeRegistry.addDescriptor(
        new EntityIdJavaType<>(EventRegistrationId.class, EventRegistrationId::new));
  }
}
