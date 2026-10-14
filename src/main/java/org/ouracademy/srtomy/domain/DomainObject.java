package org.ouracademy.srtomy.domain;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class DomainObject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  // TODO: maybe ZonedDateTime?
  // check if apply the same in LocalDate

  @CreatedDate LocalDateTime createDate;

  @LastModifiedDate LocalDateTime lastModifiedDate;
}
