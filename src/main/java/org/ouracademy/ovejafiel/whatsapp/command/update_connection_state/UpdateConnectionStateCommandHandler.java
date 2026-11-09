package org.ouracademy.ovejafiel.whatsapp.command.update_connection_state;

public interface UpdateConnectionStateCommandHandler {
  UpdateConnectionStateResult handle(UpdateConnectionStateCommand command);
}
