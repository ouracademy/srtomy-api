package org.ouracademy.srtomy.domain;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class DomainObject {

  // TODO: maybe ZonedDateTime?
  // check if apply the same in LocalDate

  @CreatedDate LocalDateTime createDate;

  @LastModifiedDate LocalDateTime lastModifiedDate;
}
