package vn.englishlearning.enrollment.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.enrollment.domain.LessonSchedule;

@Repository
public interface LessonScheduleRepository extends JpaRepository<LessonSchedule, UUID> {

  List<LessonSchedule> findByEnrollmentIdOrderByPlannedDateAsc(UUID enrollmentId);

  Optional<LessonSchedule> findByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

  void deleteByEnrollmentId(UUID enrollmentId);
}
