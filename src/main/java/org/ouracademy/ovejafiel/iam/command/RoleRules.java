package org.ouracademy.ovejafiel.iam.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** The rules a custom role must meet when it is created or edited. */
@Component
@RequiredArgsConstructor
public class RoleRules {

  private final IamRoleRepository iamRoleRepository;
  private final RoleBindingRepository roleBindingRepository;

  /**
   * Returns the distinct permissions, failing when there are none or one is not in the catalog. The
   * editor must also hold all of them on the church, so a role cannot exceed its author.
   */
  public List<String> requirePermissions(String subject, PartyId churchId, List<String> values) {
    var distinct = values == null ? List.<String>of() : values.stream().distinct().toList();
    if (distinct.isEmpty()) {
      throw new DomainValidationException("El rol debe tener al menos un permiso");
    }
    for (var value : distinct) {
      if (Permission.fromValue(value).isEmpty()) {
        throw new DomainValidationException("Permiso desconocido: " + value);
      }
    }
    var held = roleBindingRepository.findPermissions(subject, List.of(churchId.value()));
    if (!held.containsAll(distinct)) {
      throw new AccessDeniedException("No puedes asignar permisos que no tienes");
    }
    return distinct;
  }

  /**
   * Returns the trimmed name, failing when blank or already used by a predefined role or another
   * live role of the church. {@code excludedId} is the role being edited.
   */
  public String requireAvailableName(PartyId churchId, String name, IamRoleId excludedId) {
    var trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException("El nombre del rol es obligatorio");
    }
    if (iamRoleRepository.nameTaken(churchId, trimmed, excludedId)) {
      throw new DomainValidationException("Ya existe un rol llamado \"" + trimmed + "\"");
    }
    return trimmed;
  }
}
