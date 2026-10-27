package org.ouracademy.ovejafiel.accountability.command.delete_accountabilities_by_parent_and_types;

public interface DeleteAccountabilitiesByParentAndTypesCommandHandler {
  DeleteAccountabilitiesByParentAndTypesResult handle(
      DeleteAccountabilitiesByParentAndTypesCommand command);
}
