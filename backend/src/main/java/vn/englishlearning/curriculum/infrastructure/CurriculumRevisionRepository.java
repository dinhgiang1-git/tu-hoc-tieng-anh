package vn.englishlearning.curriculum.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.CurriculumRevision;

@Repository
public interface CurriculumRevisionRepository extends JpaRepository<CurriculumRevision, UUID> {

  Optional<CurriculumRevision> findByCurriculumIdAndVersion(UUID curriculumId, String version);

  Optional<CurriculumRevision> findByCurriculumCurriculumKeyAndVersion(
      String curriculumKey, String version);
}
