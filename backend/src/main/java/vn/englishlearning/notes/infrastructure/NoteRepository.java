package vn.englishlearning.notes.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.englishlearning.notes.domain.Note;

public interface NoteRepository extends JpaRepository<Note, UUID> {

  Optional<Note> findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
      UUID userId, UUID enrollmentId, UUID lessonId);

  @Query(
      "SELECT n FROM Note n WHERE n.userId = :userId "
          + "AND (:enrollmentId IS NULL OR n.enrollmentId = :enrollmentId) "
          + "AND (:lessonId IS NULL OR n.lessonId = :lessonId) "
          + "ORDER BY n.updatedAt DESC, n.id DESC")
  Page<Note> findNotes(
      @Param("userId") UUID userId,
      @Param("enrollmentId") UUID enrollmentId,
      @Param("lessonId") UUID lessonId,
      Pageable pageable);

  @Modifying(flushAutomatically = true)
  @Query(
      "UPDATE Note n SET n.content = :content, n.version = :newVersion, n.updatedAt = :now "
          + "WHERE n.id = :id AND n.version = :expectedVersion")
  int updateVersionAndContent(
      @Param("id") UUID id,
      @Param("expectedVersion") Long expectedVersion,
      @Param("newVersion") Long newVersion,
      @Param("content") String content,
      @Param("now") Instant now);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM Note n WHERE n.id = :id AND n.version = :expectedVersion")
  int deleteByIdAndVersion(@Param("id") UUID id, @Param("expectedVersion") Long expectedVersion);
}
