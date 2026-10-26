package vn.englishlearning.notes.application;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.notes.api.CreateNoteRequest;
import vn.englishlearning.notes.api.NotePageResponse;
import vn.englishlearning.notes.api.NoteResponse;
import vn.englishlearning.notes.api.UpdateNoteRequest;
import vn.englishlearning.notes.domain.Note;
import vn.englishlearning.notes.infrastructure.NoteRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

@Service
public class NoteService {

  private final NoteRepository noteRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final EntityManager entityManager;

  public NoteService(
      NoteRepository noteRepository,
      EnrollmentRepository enrollmentRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      CurrentUser currentUser,
      Clock clock,
      EntityManager entityManager) {
    this.noteRepository = noteRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.currentUser = currentUser;
    this.clock = clock;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public NotePageResponse getNotes(UUID enrollmentId, UUID lessonId, int page, int size) {
    UUID currentUserId = currentUser.requireUserId();

    if (enrollmentId != null || lessonId != null) {
      validateScope(currentUserId, enrollmentId, lessonId, null);
    }

    int safePage = Math.max(0, page);
    int safeSize = Math.min(100, Math.max(1, size));

    Page<Note> notePage =
        noteRepository.findNotes(
            currentUserId, enrollmentId, lessonId, PageRequest.of(safePage, safeSize));

    return new NotePageResponse(
        notePage.getContent().stream().map(this::toResponse).toList(),
        notePage.getNumber(),
        notePage.getSize(),
        notePage.getTotalElements(),
        notePage.getTotalPages());
  }

  @Transactional
  public NoteResponse createNote(CreateNoteRequest request) {
    if (request == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    UUID currentUserId = currentUser.requireUserId();
    validateScope(currentUserId, request.enrollmentId(), request.lessonId(), request.activityId());

    String content = request.content() != null ? request.content() : "";
    Instant now = Instant.now(clock);

    // Workspace G3-03 lesson-level note check (activityId is null, lessonId is present)
    if (request.enrollmentId() != null
        && request.lessonId() != null
        && request.activityId() == null) {
      Optional<Note> existing =
          noteRepository.findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
              currentUserId, request.enrollmentId(), request.lessonId());
      if (existing.isPresent()) {
        Note note = existing.get();
        if (Objects.equals(note.getContent(), content)) {
          return toResponse(note);
        }
        throw new DomainException(
            ErrorCode.STATE_CONFLICT,
            "Ghi chú cho bài học này đã tồn tại. Vui lòng tải lại trang.");
      }
    }

    try {
      Note note =
          new Note(
              currentUserId,
              request.enrollmentId(),
              request.lessonId(),
              request.activityId(),
              content,
              now);
      note = noteRepository.saveAndFlush(note);
      return toResponse(note);
    } catch (DataIntegrityViolationException ex) {
      if (request.enrollmentId() != null
          && request.lessonId() != null
          && request.activityId() == null) {
        throw new DomainException(
            ErrorCode.STATE_CONFLICT,
            "Ghi chú cho bài học này đã tồn tại. Vui lòng tải lại trang.");
      }
      throw ex;
    }
  }

  @Transactional
  public NoteResponse updateNote(UUID id, UpdateNoteRequest request) {
    if (request == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion phải không âm.");
    }

    UUID currentUserId = currentUser.requireUserId();
    Note note =
        noteRepository
            .findById(id)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ghi chú."));

    if (!note.getUserId().equals(currentUserId)) {
      throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ghi chú.");
    }

    if (!Objects.equals(note.getVersion(), request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT,
          "Ghi chú đã được cập nhật ở phiên khác. Vui lòng tải lại trước khi lưu.");
    }

    String newContent = request.content() != null ? request.content() : "";

    // No-op check: identical content and matching expectedVersion
    if (Objects.equals(note.getContent(), newContent)) {
      return toResponse(note);
    }

    Instant now = Instant.now(clock);
    Long newVersion = request.expectedVersion() + 1;

    entityManager.detach(note);
    int updatedRows =
        noteRepository.updateVersionAndContent(
            id, request.expectedVersion(), newVersion, newContent, now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT,
          "Ghi chú đã được cập nhật ở phiên khác. Vui lòng tải lại trước khi lưu.");
    }

    note.setContent(newContent);
    note.setVersion(newVersion);
    note.setUpdatedAt(now);

    return toResponse(note);
  }

  @Transactional
  public void deleteNote(UUID id, Long expectedVersion) {
    if (expectedVersion == null || expectedVersion < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion không hợp lệ.");
    }

    UUID currentUserId = currentUser.requireUserId();
    Note note =
        noteRepository
            .findById(id)
            .orElseThrow(
                () -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ghi chú."));

    if (!note.getUserId().equals(currentUserId)) {
      throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ghi chú.");
    }

    if (!Objects.equals(note.getVersion(), expectedVersion)) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT,
          "Ghi chú đã được cập nhật ở phiên khác. Vui lòng tải lại trước khi xóa.");
    }

    int deletedRows = noteRepository.deleteByIdAndVersion(id, expectedVersion);
    if (deletedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT,
          "Ghi chú đã được cập nhật ở phiên khác. Vui lòng tải lại trước khi xóa.");
    }
  }

  private void validateScope(
      UUID currentUserId, UUID enrollmentId, UUID lessonId, UUID activityId) {
    if (lessonId != null && enrollmentId == null) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "lessonId yêu cầu phải cung cấp enrollmentId.");
    }
    if (activityId != null && lessonId == null) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "activityId yêu cầu phải cung cấp lessonId.");
    }

    Enrollment enrollment = null;
    if (enrollmentId != null) {
      enrollment =
          enrollmentRepository
              .findById(enrollmentId)
              .orElseThrow(
                  () ->
                      new DomainException(
                          ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy enrollment."));
      if (!enrollment.getUserId().equals(currentUserId)) {
        throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy enrollment.");
      }
    }

    Lesson lesson = null;
    if (lessonId != null) {
      lesson =
          lessonRepository
              .findById(lessonId)
              .orElseThrow(
                  () ->
                      new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài học."));
      if (enrollment != null
          && !lesson
              .getWeek()
              .getPhase()
              .getRevision()
              .getId()
              .equals(enrollment.getRevisionId())) {
        throw new DomainException(
            ErrorCode.RESOURCE_NOT_FOUND, "Bài học không thuộc lộ trình đăng ký.");
      }
    }

    if (activityId != null) {
      Activity activity =
          activityRepository
              .findById(activityId)
              .orElseThrow(
                  () ->
                      new DomainException(
                          ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhiệm vụ."));
      if (lesson != null && !activity.getLesson().getId().equals(lesson.getId())) {
        throw new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Nhiệm vụ không thuộc bài học.");
      }
    }
  }

  private NoteResponse toResponse(Note note) {
    return new NoteResponse(
        note.getId(),
        note.getEnrollmentId(),
        note.getLessonId(),
        note.getActivityId(),
        note.getContent(),
        note.getVersion(),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }
}
