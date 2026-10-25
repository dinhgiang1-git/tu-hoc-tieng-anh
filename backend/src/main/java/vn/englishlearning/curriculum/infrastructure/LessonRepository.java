package vn.englishlearning.curriculum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

  List<Lesson> findByWeekIdOrderByOrderIndexAsc(UUID weekId);

  Optional<Lesson> findByWeekIdAndLessonKey(UUID weekId, String lessonKey);

  List<Lesson> findByWeekPhaseRevisionId(UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT l FROM Lesson l
      JOIN l.week w
      JOIN w.phase p
      WHERE p.revision.id = :revisionId AND l.isRequired = true
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, l.id ASC
      """)
  List<Lesson> findRequiredLessonsByRevisionIdOrdered(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT l FROM Lesson l
      JOIN l.week w
      JOIN w.phase p
      WHERE p.revision.id = :revisionId
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, l.id ASC
      """)
  List<Lesson> findLessonsByRevisionIdOrdered(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.LessonSummaryDto(
          l.id, p.revision.id, l.week.id, l.orderIndex, l.title, l.estimatedMinutes, l.isRequired
      )
      FROM Lesson l
      JOIN l.week w
      JOIN w.phase p
      WHERE p.revision.id = :revisionId
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, l.id ASC
      """)
  List<LessonSummaryDto> findLessonSummariesByRevisionId(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.LessonSummaryDto(
          l.id, p.revision.id, l.week.id, l.orderIndex, l.title, l.estimatedMinutes, l.isRequired
      )
      FROM Lesson l
      JOIN l.week w
      JOIN w.phase p
      WHERE p.revision.id IN :revisionIds
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, l.id ASC
      """)
  List<LessonSummaryDto> findLessonSummariesByRevisionIds(
      @org.springframework.data.repository.query.Param("revisionIds")
          java.util.Collection<UUID> revisionIds);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.LessonSummaryDto(
          l.id, p.revision.id, l.week.id, l.orderIndex, l.title, l.estimatedMinutes, l.isRequired
      )
      FROM Lesson l
      JOIN l.week w
      JOIN w.phase p
      WHERE l.id = :id
      """)
  Optional<LessonSummaryDto> findLessonSummaryById(
      @org.springframework.data.repository.query.Param("id") UUID id);
}
