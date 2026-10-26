package vn.englishlearning.notes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
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

class NoteServiceTest {

  private NoteRepository noteRepository;
  private EnrollmentRepository enrollmentRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private CurrentUser currentUser;
  private Clock clock;
  private EntityManager entityManager;
  private NoteService noteService;

  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final UUID lessonId = UUID.randomUUID();
  private final UUID activityId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-10-26T10:00:00Z");

  private Enrollment enrollment;
  private Lesson lesson;
  private Activity activity;

  @BeforeEach
  void setUp() {
    noteRepository = mock(NoteRepository.class);
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    currentUser = mock(CurrentUser.class);
    clock = Clock.fixed(now, ZoneOffset.UTC);
    entityManager = mock(EntityManager.class);

    when(currentUser.requireUserId()).thenReturn(userId);

    enrollment = mock(Enrollment.class);
    when(enrollment.getUserId()).thenReturn(userId);
    when(enrollment.getRevisionId()).thenReturn(revisionId);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    CurriculumRevision revision = mock(CurriculumRevision.class);
    when(revision.getId()).thenReturn(revisionId);
    CurriculumPhase phase = mock(CurriculumPhase.class);
    when(phase.getRevision()).thenReturn(revision);
    CurriculumWeek week = mock(CurriculumWeek.class);
    when(week.getPhase()).thenReturn(phase);
    lesson = mock(Lesson.class);
    when(lesson.getId()).thenReturn(lessonId);
    when(lesson.getWeek()).thenReturn(week);
    when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

    activity = mock(Activity.class);
    when(activity.getId()).thenReturn(activityId);
    when(activity.getLesson()).thenReturn(lesson);
    when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

    noteService =
        new NoteService(
            noteRepository,
            enrollmentRepository,
            lessonRepository,
            activityRepository,
            currentUser,
            clock,
            entityManager);
  }

  @Test
  @DisplayName("getNotes: returns paged notes for current user")
  void getNotes_validQuery_returnsPagedNotes() {
    Note note1 = new Note(userId, enrollmentId, lessonId, null, "Note 1", now);
    when(noteRepository.findNotes(
            eq(userId), eq(enrollmentId), eq(lessonId), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(note1), PageRequest.of(0, 20), 1));

    NotePageResponse response = noteService.getNotes(enrollmentId, lessonId, 0, 20);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).content()).isEqualTo("Note 1");
    assertThat(response.totalItems()).isEqualTo(1);
  }

  @Test
  @DisplayName("createNote: creates lesson-level note when not exists")
  void createNote_lessonLevel_success() {
    when(noteRepository.findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
            userId, enrollmentId, lessonId))
        .thenReturn(Optional.empty());

    Note savedNote = new Note(userId, enrollmentId, lessonId, null, "My Note", now);
    when(noteRepository.saveAndFlush(any(Note.class))).thenReturn(savedNote);

    CreateNoteRequest request = new CreateNoteRequest(enrollmentId, lessonId, null, "My Note");
    NoteResponse response = noteService.createNote(request);

    assertThat(response.content()).isEqualTo("My Note");
    verify(noteRepository).saveAndFlush(any(Note.class));
  }

  @Test
  @DisplayName(
      "createNote: identical content on existing lesson note returns existing note (idempotent)")
  void createNote_lessonLevel_identicalContent_returnsExisting() {
    Note existingNote = new Note(userId, enrollmentId, lessonId, null, "Existing Note", now);
    when(noteRepository.findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
            userId, enrollmentId, lessonId))
        .thenReturn(Optional.of(existingNote));

    CreateNoteRequest request =
        new CreateNoteRequest(enrollmentId, lessonId, null, "Existing Note");
    NoteResponse response = noteService.createNote(request);

    assertThat(response.content()).isEqualTo("Existing Note");
    verify(noteRepository, never()).saveAndFlush(any(Note.class));
  }

  @Test
  @DisplayName("createNote: different content on existing lesson note throws STATE_CONFLICT")
  void createNote_lessonLevel_differentContent_throwsStateConflict() {
    Note existingNote = new Note(userId, enrollmentId, lessonId, null, "Existing Note", now);
    when(noteRepository.findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
            userId, enrollmentId, lessonId))
        .thenReturn(Optional.of(existingNote));

    CreateNoteRequest request =
        new CreateNoteRequest(enrollmentId, lessonId, null, "New Different Note");

    assertThatThrownBy(() -> noteService.createNote(request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName(
      "createNote: concurrent race condition caught by unique constraint throws STATE_CONFLICT")
  void createNote_concurrentInsert_handledSafely() {
    when(noteRepository.findByUserIdAndEnrollmentIdAndLessonIdAndActivityIdIsNull(
            userId, enrollmentId, lessonId))
        .thenReturn(Optional.empty());

    when(noteRepository.saveAndFlush(any(Note.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    CreateNoteRequest request =
        new CreateNoteRequest(enrollmentId, lessonId, null, "Concurrent Note");

    assertThatThrownBy(() -> noteService.createNote(request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("updateNote: stale expectedVersion throws VERSION_CONFLICT")
  void updateNote_staleVersion_throwsVersionConflict() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Old Content", now);
    note.setVersion(2L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

    UpdateNoteRequest request = new UpdateNoteRequest("New Content", 1L);

    assertThatThrownBy(() -> noteService.updateNote(noteId, request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("updateNote: identical content is no-op and does not increment version")
  void updateNote_identicalContent_noOp() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Same Content", now);
    note.setVersion(1L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

    UpdateNoteRequest request = new UpdateNoteRequest("Same Content", 1L);
    NoteResponse response = noteService.updateNote(noteId, request);

    assertThat(response.content()).isEqualTo("Same Content");
    assertThat(response.version()).isEqualTo(1L);
    verify(noteRepository, never()).updateVersionAndContent(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("updateNote: changed content increments version and updates updatedAt")
  void updateNote_changedContent_incrementsVersion() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Old Content", now);
    ReflectionTestUtils.setField(note, "id", noteId);
    note.setVersion(1L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(noteRepository.updateVersionAndContent(
            eq(noteId), eq(1L), eq(2L), eq("New Content"), eq(now)))
        .thenReturn(1);

    UpdateNoteRequest request = new UpdateNoteRequest("New Content", 1L);
    NoteResponse response = noteService.updateNote(noteId, request);

    assertThat(response.content()).isEqualTo("New Content");
    assertThat(response.version()).isEqualTo(2L);
    verify(entityManager).detach(note);
  }

  @Test
  @DisplayName("updateNote: empty string content is allowed to clear note")
  void updateNote_emptyString_clearsNote() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Old Content", now);
    ReflectionTestUtils.setField(note, "id", noteId);
    note.setVersion(1L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(noteRepository.updateVersionAndContent(eq(noteId), eq(1L), eq(2L), eq(""), eq(now)))
        .thenReturn(1);

    UpdateNoteRequest request = new UpdateNoteRequest("", 1L);
    NoteResponse response = noteService.updateNote(noteId, request);

    assertThat(response.content()).isEmpty();
    assertThat(response.version()).isEqualTo(2L);
  }

  @Test
  @DisplayName("deleteNote: stale expectedVersion throws VERSION_CONFLICT")
  void deleteNote_staleVersion_throwsVersionConflict() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Content", now);
    note.setVersion(2L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));

    assertThatThrownBy(() -> noteService.deleteNote(noteId, 1L))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("deleteNote: matching expectedVersion deletes note successfully")
  void deleteNote_matchingVersion_success() {
    UUID noteId = UUID.randomUUID();
    Note note = new Note(userId, enrollmentId, lessonId, null, "Content", now);
    note.setVersion(1L);
    when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
    when(noteRepository.deleteByIdAndVersion(noteId, 1L)).thenReturn(1);

    noteService.deleteNote(noteId, 1L);

    verify(noteRepository).deleteByIdAndVersion(noteId, 1L);
  }

  @Test
  @DisplayName("scopeValidation: non-owner enrollment throws RESOURCE_NOT_FOUND")
  void scopeValidation_otherUserEnrollment_throwsNotFound() {
    Enrollment otherUserEnrollment = mock(Enrollment.class);
    when(otherUserEnrollment.getUserId()).thenReturn(otherUserId);
    UUID otherEnrollmentId = UUID.randomUUID();
    when(enrollmentRepository.findById(otherEnrollmentId))
        .thenReturn(Optional.of(otherUserEnrollment));

    CreateNoteRequest request = new CreateNoteRequest(otherEnrollmentId, null, null, "Note");

    assertThatThrownBy(() -> noteService.createNote(request))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).getErrorCode())
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }
}
