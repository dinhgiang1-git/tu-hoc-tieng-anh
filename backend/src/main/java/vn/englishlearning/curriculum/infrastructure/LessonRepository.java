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
}
