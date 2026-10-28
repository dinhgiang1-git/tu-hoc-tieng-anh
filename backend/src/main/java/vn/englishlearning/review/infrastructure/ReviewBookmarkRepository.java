package vn.englishlearning.review.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.englishlearning.review.domain.ReviewBookmark;

@Repository
public interface ReviewBookmarkRepository extends JpaRepository<ReviewBookmark, UUID> {

  List<ReviewBookmark> findByEnrollmentIdOrderByCreatedAtDesc(UUID enrollmentId);

  List<ReviewBookmark> findByUserIdOrderByCreatedAtDesc(UUID userId);

  @Query(
      "SELECT b FROM ReviewBookmark b WHERE b.enrollmentId = :enrollmentId AND b.lessonId = :lessonId"
          + " AND ((:activityId IS NULL AND b.activityId IS NULL) OR b.activityId = :activityId)")
  Optional<ReviewBookmark> findBookmark(
      @Param("enrollmentId") UUID enrollmentId,
      @Param("lessonId") UUID lessonId,
      @Param("activityId") UUID activityId);

  Optional<ReviewBookmark> findByIdAndUserId(UUID id, UUID userId);
}
