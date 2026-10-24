package vn.englishlearning.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.identity.domain.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

  Optional<AppUser> findByUsername(String username);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}
