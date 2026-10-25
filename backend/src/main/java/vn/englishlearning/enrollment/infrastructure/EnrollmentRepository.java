package vn.englishlearning.enrollment.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  boolean existsByIdAndUserId(UUID id, UUID userId);

  Optional<Enrollment> findByIdAndUserId(UUID id, UUID userId);

  boolean existsByUserIdAndCurriculumIdAndStatusIn(
      UUID userId, UUID curriculumId, Collection<EnrollmentStatus> statuses);

  List<Enrollment> findByUserIdAndCurriculumIdAndStatusInAndIdNot(
      UUID userId, UUID curriculumId, Collection<EnrollmentStatus> statuses, UUID excludeId);

  List<Enrollment> findByUserIdOrderByCreatedAtDesc(UUID userId);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE Enrollment e SET e.version = :newVersion, e.status = :status, e.completedAt = :completedAt, e.updatedAt = :now "
          + "WHERE e.id = :id AND e.version = :expectedVersion")
  int updateVersionAndState(
      @Param("id") UUID id,
      @Param("expectedVersion") Long expectedVersion,
      @Param("newVersion") Long newVersion,
      @Param("status") EnrollmentStatus status,
      @Param("completedAt") Instant completedAt,
      @Param("now") Instant now);
}
