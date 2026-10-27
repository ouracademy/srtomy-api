package org.ouracademy.ovejafiel.shared.domain;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.complaint.domain.ComplaintId;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.EventSeriesId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.domain.UserPersonLinkId;

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
