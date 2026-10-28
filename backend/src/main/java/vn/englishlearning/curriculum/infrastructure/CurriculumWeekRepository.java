package vn.englishlearning.curriculum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.CurriculumWeek;

@Repository
public interface CurriculumWeekRepository extends JpaRepository<CurriculumWeek, UUID> {

  List<CurriculumWeek> findByPhaseIdOrderByOrderIndexAsc(UUID phaseId);

  Optional<CurriculumWeek> findByPhaseIdAndWeekKey(UUID phaseId, String weekKey);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT w FROM CurriculumWeek w
      JOIN w.phase p
      WHERE p.revision.id = :revisionId
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, w.weekNumber ASC
      """)
  List<CurriculumWeek> findWeeksByRevisionIdOrdered(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);
}
