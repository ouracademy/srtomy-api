package org.ouracademy.srtomy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ouracademy.srtomy.complaint.ComplaintId;
import org.ouracademy.srtomy.event.EventId;
import org.ouracademy.srtomy.person.AccountabilityId;
import org.ouracademy.srtomy.person.AccountabilityTypeId;
import org.ouracademy.srtomy.person.PartyId;

/**
 * Every {@link EntityId} record (PartyId, EventId, ...) is expected to behave the same way; this
 * exercises that shared contract once instead of duplicating the same tests per record.
 */
class EntityIdContractTests {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static Stream<Arguments> idTypes() {
    return Stream.of(
        idType("PartyId", PartyId::generate, PartyId::new),
        idType("EventId", EventId::generate, EventId::new),
        idType("AccountabilityId", AccountabilityId::generate, AccountabilityId::new),
        idType("AccountabilityTypeId", AccountabilityTypeId::generate, AccountabilityTypeId::new),
        idType("ComplaintId", ComplaintId::generate, ComplaintId::new));
  }

  private static Arguments idType(
      String name, Supplier<? extends EntityId> generate, Function<UUID, ? extends EntityId> of) {
    return Arguments.of(name, generate, of);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("idTypes")
  void generateProducesAUuidV7(
      String name, Supplier<? extends EntityId> generate, Function<UUID, ? extends EntityId> of) {
    assertThat(generate.get().value().version()).isEqualTo(7);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("idTypes")
  void generateProducesDistinctValues(
      String name, Supplier<? extends EntityId> generate, Function<UUID, ? extends EntityId> of) {
    assertThat(generate.get()).isNotEqualTo(generate.get());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("idTypes")
  void sameUuidMeansEqualIds(
      String name, Supplier<? extends EntityId> generate, Function<UUID, ? extends EntityId> of) {
    var uuid = UUID.randomUUID();

    assertThat(of.apply(uuid)).isEqualTo(of.apply(uuid));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("idTypes")
  void serializesAsAPlainUuidString(
      String name, Supplier<? extends EntityId> generate, Function<UUID, ? extends EntityId> of)
      throws Exception {
    var id = generate.get();

    var json = OBJECT_MAPPER.writeValueAsString(id);

    assertThat(json).isEqualTo("\"" + id.value() + "\"");
  }
}
