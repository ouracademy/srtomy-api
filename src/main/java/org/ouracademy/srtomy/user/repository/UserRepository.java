package org.ouracademy.srtomy.user.repository;

import java.util.Optional;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.domain.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UserId> {
  Optional<AppUser> findBySubject(String subject);

  Optional<AppUser> findFirstByEmailIgnoreCaseAndEmailVerifiedTrueOrderByCreateDate(String email);
}
