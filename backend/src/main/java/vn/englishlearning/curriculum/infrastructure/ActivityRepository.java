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
}
