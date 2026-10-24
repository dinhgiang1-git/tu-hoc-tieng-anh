package vn.englishlearning.identity.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.identity.domain.LearnerPreferences;

@Repository
public interface LearnerPreferencesRepository extends JpaRepository<LearnerPreferences, UUID> {}
