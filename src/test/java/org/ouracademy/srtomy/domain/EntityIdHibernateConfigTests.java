package org.ouracademy.srtomy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.junit.jupiter.api.Test;

class EntityIdHibernateConfigTests {

  @Test
  void registersTheEntityIdTypeContributor() {
    var hibernateProperties = new HashMap<String, Object>();

    new EntityIdHibernateConfig().customize(hibernateProperties);

    var contributorList =
        (TypeContributorList) hibernateProperties.get(JpaSettings.TYPE_CONTRIBUTORS);
    assertThat(contributorList.getTypeContributors())
        .singleElement()
        .isInstanceOf(EntityIdTypeContributor.class);
  }
}
