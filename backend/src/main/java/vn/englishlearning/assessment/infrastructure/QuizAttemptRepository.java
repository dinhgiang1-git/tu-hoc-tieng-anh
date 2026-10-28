package vn.englishlearning.assessment.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.englishlearning.assessment.domain.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

  Optional<QuizAttempt> findTopByEnrollmentIdAndActivityIdOrderBySubmittedAtDesc(
      UUID enrollmentId, UUID activityId);

  List<QuizAttempt> findByEnrollmentIdAndActivityIdOrderBySubmittedAtDesc(
      UUID enrollmentId, UUID activityId);

  int countByEnrollmentIdAndActivityId(UUID enrollmentId, UUID activityId);

  boolean existsByEnrollmentIdAndActivityIdAndPassedTrue(UUID enrollmentId, UUID activityId);

  @org.springframework.data.jpa.repository.Query(
      "SELECT DISTINCT a FROM QuizAttempt a LEFT JOIN FETCH a.answers WHERE a.enrollmentId = :enrollmentId ORDER BY a.submittedAt DESC")
  List<QuizAttempt> findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(
      @org.springframework.data.repository.query.Param("enrollmentId") UUID enrollmentId);
}
