package org.ouracademy.ovejafiel.event.command.edit_event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;

class EditEventCommandHandlerImplTests {

  @Test
  void editingIsNotImplementedYet() {
    var handler = new EditEventCommandHandlerImpl();

    assertThatThrownBy(
            () ->
                handler.handle(
                    new EditEventCommand(EventId.generate(), EventChangeOption.THIS_EVENT)))
        .isInstanceOf(NotImplementedException.class)
        .hasMessage("TODO:");
  }
}
