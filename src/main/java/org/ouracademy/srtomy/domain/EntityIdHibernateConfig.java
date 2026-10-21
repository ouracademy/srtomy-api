package org.ouracademy.srtomy.domain;

import java.util.List;
import java.util.Map;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Registers {@link EntityIdTypeContributor} through Spring Boot's own Hibernate customization hook,
 * instead of a {@code META-INF/services} ServiceLoader file.
 */
@Component
public class EntityIdHibernateConfig implements HibernatePropertiesCustomizer {

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(
        JpaSettings.TYPE_CONTRIBUTORS,
        (TypeContributorList) () -> List.of(new EntityIdTypeContributor()));
  }
}
