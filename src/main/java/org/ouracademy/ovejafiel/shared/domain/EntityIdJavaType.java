package org.ouracademy.ovejafiel.shared.domain;

import java.util.UUID;
import java.util.function.Function;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/** Maps a {@link EntityId} record (PersonId, EventId, ...) to/from a plain UUID column. */
public class EntityIdJavaType<T extends EntityId> extends AbstractClassJavaType<T> {

  private final Function<UUID, T> factory;

  public EntityIdJavaType(Class<T> javaType, Function<UUID, T> factory) {
    super(javaType);
    this.factory = factory;
  }

  @Override
  public T fromString(CharSequence sequence) {
    return sequence == null ? null : factory.apply(UUID.fromString(sequence.toString()));
  }

  @Override
  public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
    return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(SqlTypes.UUID);
  }

  @Override
  public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
    if (value == null) {
      return null;
    }
    if (UUID.class.isAssignableFrom(type)) {
      return type.cast(value.value());
    }
    if (String.class.isAssignableFrom(type)) {
      return type.cast(value.value().toString());
    }
    throw unknownUnwrap(type);
  }

  @Override
  public <X> T wrap(X value, WrapperOptions options) {
    if (value == null) {
      return null;
    }
    if (getJavaTypeClass().isInstance(value)) {
      return getJavaTypeClass().cast(value);
    }
    if (value instanceof UUID uuid) {
      return factory.apply(uuid);
    }
    if (value instanceof String s) {
      return factory.apply(UUID.fromString(s));
    }
    throw unknownWrap(value.getClass());
  }
}
