package vn.englishlearning.curriculum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.Activity;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

  List<Activity> findByLessonIdOrderByOrderIndexAsc(UUID lessonId);

  Optional<Activity> findByLessonIdAndActivityKey(UUID lessonId, String activityKey);

  List<Activity> findByLessonWeekPhaseRevisionId(UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto(
          a.id, a.lesson.id, a.orderIndex, a.isRequired
      )
      FROM Activity a
      WHERE a.lesson.week.phase.revision.id = :revisionId
      ORDER BY a.orderIndex ASC
      """)
  List<ActivitySummaryDto> findActivitySummariesByRevisionId(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto(
          a.id, a.lesson.id, a.orderIndex, a.isRequired
      )
      FROM Activity a
      WHERE a.lesson.week.phase.revision.id IN :revisionIds
      ORDER BY a.orderIndex ASC
      """)
  List<ActivitySummaryDto> findActivitySummariesByRevisionIds(
      @org.springframework.data.repository.query.Param("revisionIds")
          java.util.Collection<UUID> revisionIds);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.ActivityProjectionDto(
          a.id, a.lesson.id, a.orderIndex, a.title, a.activityType, a.isRequired, a.completionPolicy
      )
      FROM Activity a
      WHERE a.lesson.id = :lessonId
      ORDER BY a.orderIndex ASC, a.id ASC
      """)
  List<ActivityProjectionDto> findActivityProjectionsByLessonId(
      @org.springframework.data.repository.query.Param("lessonId") UUID lessonId);
}
