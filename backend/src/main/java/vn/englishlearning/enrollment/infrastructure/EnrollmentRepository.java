package vn.englishlearning.enrollment.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  boolean existsByIdAndUserId(UUID id, UUID userId);

  Optional<Enrollment> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndCurriculumIdAndStatusIn(
      UUID userId, UUID curriculumId, Collection<EnrollmentStatus> statuses);

  List<Enrollment> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
