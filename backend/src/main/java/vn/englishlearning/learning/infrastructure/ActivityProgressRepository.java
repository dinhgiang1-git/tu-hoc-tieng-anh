package vn.englishlearning.learning.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;

@Repository
public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, UUID> {

  List<ActivityProgress> findByEnrollmentId(UUID enrollmentId);

  Optional<ActivityProgress> findByEnrollmentIdAndActivityId(UUID enrollmentId, UUID activityId);

  List<ActivityProgress> findByEnrollmentIdAndActivityIdIn(
      UUID enrollmentId, Collection<UUID> activityIds);

  List<ActivityProgress> findByEnrollmentIdAndStatusIn(
      UUID enrollmentId, Collection<ActivityStatus> statuses);

  long countByEnrollmentIdAndStatus(UUID enrollmentId, ActivityStatus status);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.learning.infrastructure.ActivityProgressDto(
          ap.enrollmentId, ap.activityId, ap.status
      )
      FROM ActivityProgress ap
      WHERE ap.enrollmentId = :enrollmentId
      """)
  List<ActivityProgressDto> findProgressDtosByEnrollmentId(
      @org.springframework.data.repository.query.Param("enrollmentId") UUID enrollmentId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.learning.infrastructure.ActivityProgressDto(
          ap.enrollmentId, ap.activityId, ap.status
      )
      FROM ActivityProgress ap
      WHERE ap.enrollmentId IN :enrollmentIds
      """)
  List<ActivityProgressDto> findProgressDtosByEnrollmentIds(
      @org.springframework.data.repository.query.Param("enrollmentIds")
          Collection<UUID> enrollmentIds);
}
