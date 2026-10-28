package vn.englishlearning.review.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.review.domain.ReviewResolution;

@Repository
public interface ReviewResolutionRepository extends JpaRepository<ReviewResolution, UUID> {

  Optional<ReviewResolution> findByEnrollmentIdAndQuestionId(UUID enrollmentId, UUID questionId);

  List<ReviewResolution> findByEnrollmentId(UUID enrollmentId);
}
