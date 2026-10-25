package vn.englishlearning.learning.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.learning.domain.LearningEvent;

@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, UUID> {

  List<LearningEvent> findByEnrollmentIdOrderByOccurredAtDesc(UUID enrollmentId);
}
