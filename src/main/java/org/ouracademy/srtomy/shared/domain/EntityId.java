package org.ouracademy.srtomy.shared.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;

public interface EntityId {
  @JsonValue
  UUID value();
}
