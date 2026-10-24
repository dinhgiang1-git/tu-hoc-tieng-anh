package vn.englishlearning.curriculum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.CurriculumPhase;

@Repository
public interface CurriculumPhaseRepository extends JpaRepository<CurriculumPhase, UUID> {

  List<CurriculumPhase> findByRevisionIdOrderByOrderIndexAsc(UUID revisionId);

  Optional<CurriculumPhase> findByRevisionIdAndPhaseKey(UUID revisionId, String phaseKey);
}
