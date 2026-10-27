package org.ouracademy.ovejafiel.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class EntityIdJavaTypeTests {

  private final EntityIdJavaType<PartyId> javaType =
      new EntityIdJavaType<>(PartyId.class, PartyId::new);

  @Test
  void wrapsAUuidIntoTheTypedId() {
    var uuid = UUID.randomUUID();

    assertThat(javaType.wrap(uuid, null)).isEqualTo(new PartyId(uuid));
  }

  @Test
  void wrapsAStringIntoTheTypedId() {
    var uuid = UUID.randomUUID();

    assertThat(javaType.wrap(uuid.toString(), null)).isEqualTo(new PartyId(uuid));
  }

  @Test
  void wrapIsIdempotentForAnAlreadyTypedId() {
    var id = PartyId.generate();

    assertThat(javaType.wrap(id, null)).isEqualTo(id);
  }

  @Test
  void wrapOfNullIsNull() {
    assertThat(javaType.wrap(null, null)).isNull();
  }

  @Test
  void wrapRejectsAnUnsupportedType() {
    assertThatThrownBy(() -> javaType.wrap(42, null)).isInstanceOf(HibernateException.class);
  }

  @Test
  void unwrapsToAUuidColumnValue() {
    var uuid = UUID.randomUUID();

    assertThat(javaType.unwrap(new PartyId(uuid), UUID.class, null)).isEqualTo(uuid);
  }

  @Test
  void unwrapsToAStringColumnValue() {
    var uuid = UUID.randomUUID();

    assertThat(javaType.unwrap(new PartyId(uuid), String.class, null)).isEqualTo(uuid.toString());
  }

  @Test
  void unwrapOfNullIsNull() {
    assertThat(javaType.unwrap(null, UUID.class, null)).isNull();
  }

  @Test
  void unwrapRejectsAnUnsupportedType() {
    var id = PartyId.generate();

    assertThatThrownBy(() -> javaType.unwrap(id, Integer.class, null))
        .isInstanceOf(HibernateException.class);
  }

  @Test
  void fromStringParsesTheUuid() {
    var uuid = UUID.randomUUID();

    assertThat(javaType.fromString(uuid.toString())).isEqualTo(new PartyId(uuid));
  }

  @Test
  void fromStringOfNullIsNull() {
    assertThat(javaType.fromString(null)).isNull();
  }

  @Test
  void recommendedJdbcTypeIsUuid() {
    var indicators = mock(JdbcTypeIndicators.class);
    var typeConfiguration = mock(TypeConfiguration.class);
    var jdbcTypeRegistry = mock(JdbcTypeRegistry.class);
    var uuidJdbcType = mock(JdbcType.class);
    when(indicators.getTypeConfiguration()).thenReturn(typeConfiguration);
    when(typeConfiguration.getJdbcTypeRegistry()).thenReturn(jdbcTypeRegistry);
    when(jdbcTypeRegistry.getDescriptor(SqlTypes.UUID)).thenReturn(uuidJdbcType);

    assertThat(javaType.getRecommendedJdbcType(indicators)).isSameAs(uuidJdbcType);
  }
}
