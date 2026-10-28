package vn.englishlearning.enrollment.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.englishlearning.enrollment.domain.LessonSchedule;

@Repository
public interface LessonScheduleRepository extends JpaRepository<LessonSchedule, UUID> {

  List<LessonSchedule> findByEnrollmentIdOrderByPlannedDateAsc(UUID enrollmentId);

  Optional<LessonSchedule> findByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

  void deleteByEnrollmentId(UUID enrollmentId);

  @Query("SELECT MAX(s.plannedDate) FROM LessonSchedule s WHERE s.enrollmentId = :enrollmentId")
  Optional<LocalDate> findMaxPlannedDateByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

  @Query(
      """
      SELECT new vn.englishlearning.enrollment.infrastructure.LessonScheduleItemDto(
          s.lessonId,
          l.title,
          s.plannedDate,
          s.originalDate,
          s.completedAt
      )
      FROM LessonSchedule s
      JOIN Lesson l ON s.lessonId = l.id
      JOIN l.week w
      JOIN w.phase p
      WHERE s.enrollmentId = :enrollmentId
        AND s.plannedDate >= :from
        AND s.plannedDate <= :to
      ORDER BY s.plannedDate ASC, p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, s.id ASC
      """)
  List<LessonScheduleItemDto> findScheduleItemsByEnrollmentIdAndRange(
      @Param("enrollmentId") UUID enrollmentId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  @Query(
      """
      SELECT new vn.englishlearning.enrollment.infrastructure.LessonScheduleDetailDto(
          s.id,
          s.lessonId,
          l.title,
          s.plannedDate,
          s.originalDate,
          s.completedAt,
          l.isRequired
      )
      FROM LessonSchedule s
      JOIN Lesson l ON s.lessonId = l.id
      JOIN l.week w
      JOIN w.phase p
      WHERE s.enrollmentId = :enrollmentId
      ORDER BY p.orderIndex ASC, w.orderIndex ASC, l.orderIndex ASC, s.id ASC
      """)
  List<LessonScheduleDetailDto> findAllScheduleDetailsByEnrollmentIdOrdered(
      @Param("enrollmentId") UUID enrollmentId);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE LessonSchedule s SET s.plannedDate = :plannedDate, s.updatedAt = :now WHERE s.id = :id")
  int updatePlannedDate(
      @Param("id") UUID id, @Param("plannedDate") LocalDate plannedDate, @Param("now") Instant now);
}
