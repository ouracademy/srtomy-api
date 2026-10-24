package org.ouracademy.srtomy.iam.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Who may grant or revoke which role, so nobody can hand out more access than they hold. Owner is
 * special: only an Owner grants or revokes it, even though an Administrator has the same
 * permissions.
 */
@Component
@RequiredArgsConstructor
public class GrantRules {

  private final RoleBindingRepository roleBindingRepository;

  /**
   * Fails unless the caller holds the role on the node or an ancestor, or already holds every one
   * of its permissions there.
   */
  public void requireCanGrant(
      String subject, IamRoleId roleId, List<String> rolePermissions, PartyId nodeId) {
    requireOwnerForOwnerRole(subject, roleId, nodeId);
    if (roleId.equals(PredefinedRoles.OWNER)
        || roleBindingRepository.holdsRole(subject, roleId.value(), nodeId.value())) {
      return;
    }
    var held = roleBindingRepository.findPermissions(subject, List.of(nodeId.value()));
    if (!held.containsAll(rolePermissions)) {
      throw new AccessDeniedException("No puedes otorgar un rol con permisos que no tienes");
    }
  }

  /**
   * Fails unless the caller already holds every one of {@code permissions} on the node, so nobody
   * hands out (through a position) more than they have.
   */
  public void requireHoldsAll(String subject, List<String> permissions, PartyId nodeId) {
    var held = roleBindingRepository.findPermissions(subject, List.of(nodeId.value()));
    if (!held.containsAll(permissions)) {
      throw new AccessDeniedException("No puedes dar permisos que no tienes");
    }
  }

  /** Revoking only needs the Owner exception, since {@code iam.manage} is checked by the caller. */
  public void requireCanRevoke(String subject, IamRoleId roleId, PartyId nodeId) {
    requireOwnerForOwnerRole(subject, roleId, nodeId);
  }

  private void requireOwnerForOwnerRole(String subject, IamRoleId roleId, PartyId nodeId) {
    if (roleId.equals(PredefinedRoles.OWNER)
        && !roleBindingRepository.holdsRole(subject, roleId.value(), nodeId.value())) {
      throw new AccessDeniedException("Solo un Propietario puede otorgar o quitar ese rol");
    }
  }
}
