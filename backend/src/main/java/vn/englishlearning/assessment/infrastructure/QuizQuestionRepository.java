package vn.englishlearning.assessment.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.englishlearning.assessment.domain.QuizQuestion;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

  @Query(
      "SELECT DISTINCT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.activityId = :activityId ORDER BY q.orderIndex ASC")
  List<QuizQuestion> findByActivityIdWithOptions(@Param("activityId") UUID activityId);

  List<QuizQuestion> findByActivityIdOrderByOrderIndexAsc(UUID activityId);

  long countByActivityId(UUID activityId);
}
