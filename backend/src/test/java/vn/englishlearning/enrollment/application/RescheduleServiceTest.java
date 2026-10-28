package vn.englishlearning.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.enrollment.api.EnrollmentRequests.LifecycleTransitionRequest;
import vn.englishlearning.enrollment.api.EnrollmentRequests.ScheduleCommitRequest;
import vn.englishlearning.enrollment.api.EnrollmentRequests.SchedulePreviewRequest;
import vn.englishlearning.enrollment.api.EnrollmentResponses.EnrollmentResponse;
import vn.englishlearning.enrollment.api.EnrollmentResponses.SchedulePreviewResponse;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleDetailDto;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.application.IdempotencyService;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.security.CurrentUser;

class RescheduleServiceTest {

  private EnrollmentRepository enrollmentRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private AppUserRepository userRepository;
  private IdempotencyService idempotencyService;
  private CurrentUser currentUser;
  private EntityManager entityManager;
  private Clock clock;
  private RescheduleService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  // 2026-10-26 is a MONDAY
  private final Instant fixedInstant = Instant.parse("2026-10-26T08:00:00Z");

  @BeforeEach
  void setUp() {
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    userRepository = mock(AppUserRepository.class);
    idempotencyService = mock(IdempotencyService.class);
    currentUser = mock(CurrentUser.class);
    entityManager = mock(EntityManager.class);
    clock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Ho_Chi_Minh"));

    when(currentUser.requireUserId()).thenReturn(userId);

    service =
        new RescheduleService(
            enrollmentRepository,
            lessonScheduleRepository,
            userRepository,
            idempotencyService,
            currentUser,
            entityManager,
            clock);
  }

  private Enrollment createEnrollment(EnrollmentStatus status, Long version) {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            fixedInstant);
    enrollment.setStatus(status);
    enrollment.setVersion(version);
    org.springframework.test.util.ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    return enrollment;
  }

  @Test
  @DisplayName(
      "previewReschedule: separates completed and uncompleted, keeps completed dates, shifts uncompleted, keeps originalDate")
  void previewReschedule_success() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID l1 = UUID.randomUUID();
    UUID l2 = UUID.randomUUID();
    UUID l3 = UUID.randomUUID();

    // Lesson 1 was completed on 2026-10-26
    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            l1,
            "Lesson 1",
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            fixedInstant,
            true);
    // Lesson 2 was planned for 2026-10-28, uncompleted
    LessonScheduleDetailDto s2 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            l2,
            "Lesson 2",
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            null,
            true);
    // Lesson 3 was planned for 2026-10-30, uncompleted
    LessonScheduleDetailDto s3 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            l3,
            "Lesson 3",
            LocalDate.of(2026, 10, 30),
            LocalDate.of(2026, 10, 30),
            null,
            true);

    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1, s2, s3));

    // Request new study days: TUESDAY, THURSDAY starting from tomorrow (2026-10-27, Tuesday)
    SchedulePreviewRequest request =
        new SchedulePreviewRequest(
            LocalDate.of(2026, 10, 27), List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 45);

    SchedulePreviewResponse preview = service.previewReschedule(enrollmentId, request);

    assertThat(preview.enrollmentVersion()).isEqualTo(1L);
    // Lesson 2 -> Tuesday 2026-10-27; Lesson 3 -> Thursday 2026-10-29
    assertThat(preview.proposedEndDate()).isEqualTo(LocalDate.of(2026, 10, 29));
    assertThat(preview.modifiedSchedules()).hasSize(2);

    var m1 = preview.modifiedSchedules().get(0);
    assertThat(m1.lessonId()).isEqualTo(l2);
    assertThat(m1.previousPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 28));
    assertThat(m1.newPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    assertThat(m1.originalDate()).isEqualTo(LocalDate.of(2026, 10, 28));

    var m2 = preview.modifiedSchedules().get(1);
    assertThat(m2.lessonId()).isEqualTo(l3);
    assertThat(m2.previousPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 30));
    assertThat(m2.newPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 29));
    assertThat(m2.originalDate()).isEqualTo(LocalDate.of(2026, 10, 30));
  }

  @Test
  @DisplayName("previewReschedule: proposedEndDate uses MAX of completed and newly planned dates")
  void previewReschedule_proposedEndDate_takesMaxOfCompletedAndUncompleted() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Suppose lesson 1 was completed far in the future or previously planned late (e.g. 2026-11-15)
    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Lesson 1",
            LocalDate.of(2026, 11, 15),
            LocalDate.of(2026, 10, 26),
            fixedInstant,
            true);
    // Lesson 2 is uncompleted, rescheduled to 2026-10-27
    LessonScheduleDetailDto s2 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Lesson 2",
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            null,
            true);

    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1, s2));

    SchedulePreviewRequest request =
        new SchedulePreviewRequest(LocalDate.of(2026, 10, 27), List.of(DayOfWeek.TUESDAY), 30);

    SchedulePreviewResponse preview = service.previewReschedule(enrollmentId, request);
    // Uncompleted end date is 2026-10-27, completed was 2026-11-15 -> MAX is 2026-11-15
    assertThat(preview.proposedEndDate()).isEqualTo(LocalDate.of(2026, 11, 15));
  }

  @Test
  @DisplayName("previewReschedule: rejects resumeDate before today in enrollment timezone")
  void previewReschedule_rejectsPastDate() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // today is 2026-10-26, request 2026-10-25
    SchedulePreviewRequest request =
        new SchedulePreviewRequest(LocalDate.of(2026, 10, 25), List.of(DayOfWeek.MONDAY), 30);

    assertThatThrownBy(() -> service.previewReschedule(enrollmentId, request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getErrorCode() == ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("previewReschedule: ABANDONED enrollment returns 409 STATE_CONFLICT")
  void previewReschedule_nonActive_stateConflict() {
    Enrollment abandonedEnrollment = createEnrollment(EnrollmentStatus.ABANDONED, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(abandonedEnrollment));

    SchedulePreviewRequest request =
        new SchedulePreviewRequest(LocalDate.of(2026, 10, 26), List.of(DayOfWeek.MONDAY), 30);

    assertThatThrownBy(() -> service.previewReschedule(enrollmentId, request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getErrorCode() == ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("previewReschedule: PAUSED enrollment allows preview calculation")
  void previewReschedule_pausedStatusAllowed_success() {
    Enrollment pausedEnrollment = createEnrollment(EnrollmentStatus.PAUSED, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(pausedEnrollment));
    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of());

    SchedulePreviewRequest request =
        new SchedulePreviewRequest(LocalDate.of(2026, 10, 26), List.of(DayOfWeek.MONDAY), 30);

    SchedulePreviewResponse response = service.previewReschedule(enrollmentId, request);
    assertThat(response).isNotNull();
    assertThat(response.enrollmentVersion()).isEqualTo(1L);
  }

  @Test
  @DisplayName("commitReschedule: replayed before checking version or temporal condition")
  void commitReschedule_replaysBeforeVersionOrDateCheck() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("hashed-payload");

    IdempotencyRecord cachedRecord = mock(IdempotencyRecord.class);
    when(idempotencyService.findExisting(userId, "COMMIT_RESCHEDULE", rawKey))
        .thenReturn(Optional.of(cachedRecord));

    EnrollmentResponse cachedResponse =
        new EnrollmentResponse(
            enrollmentId,
            userId,
            curriculumId,
            revisionId,
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            5L);

    when(idempotencyService.replay(cachedRecord, "hashed-payload", EnrollmentResponse.class))
        .thenReturn(cachedResponse);

    // Stale expectedVersion and past resumeDate: should NOT throw because replay happens first!
    ScheduleCommitRequest request =
        new ScheduleCommitRequest(LocalDate.of(2020, 1, 1), List.of(DayOfWeek.MONDAY), 30, 1L);

    EnrollmentResponse result = service.commitReschedule(rawKey, enrollmentId, request);
    assertThat(result).isSameAs(cachedResponse);
    verify(enrollmentRepository, never()).findById(any());
  }

  @Test
  @DisplayName("commitReschedule: rejects stale expectedVersion with VERSION_CONFLICT")
  void commitReschedule_staleVersion_conflict() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.findExisting(userId, "COMMIT_RESCHEDULE", rawKey))
        .thenReturn(Optional.empty());

    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 3L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    ScheduleCommitRequest request =
        new ScheduleCommitRequest(
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY),
            30,
            2L); // expected 2L, current is 3L

    assertThatThrownBy(() -> service.commitReschedule(rawKey, enrollmentId, request))
        .isInstanceOf(DomainException.class)
        .matches(ex -> ((DomainException) ex).getErrorCode() == ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName("commitReschedule: true no-op does not bump version or update schedules")
  void commitReschedule_trueNoop_noBump() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.findExisting(userId, "COMMIT_RESCHEDULE", rawKey))
        .thenReturn(Optional.empty());

    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID l1 = UUID.randomUUID();
    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            l1,
            "Lesson 1",
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            null,
            true);

    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1));

    // Request same studyDays, same target minutes (30), starting today (Monday 2026-10-26) ->
    // planned date remains 2026-10-26
    ScheduleCommitRequest request =
        new ScheduleCommitRequest(
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            1L);

    EnrollmentResponse response = service.commitReschedule(rawKey, enrollmentId, request);

    assertThat(response.version()).isEqualTo(1L);
    verify(lessonScheduleRepository, never()).updatePlannedDate(any(), any(), any());
    verify(enrollmentRepository, never())
        .updateScheduleConfigAndVersion(any(), any(), any(), any(), anyInt(), any());
    verify(idempotencyService)
        .record(eq(userId), eq("COMMIT_RESCHEDULE"), eq(rawKey), any(), eq(200), any());
  }

  @Test
  @DisplayName(
      "commitReschedule: successful commit updates schedules, bumps version via CAS, records idempotency")
  void commitReschedule_successfulCommit() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.findExisting(userId, "COMMIT_RESCHEDULE", rawKey))
        .thenReturn(Optional.empty());

    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 2L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID sId = UUID.randomUUID();
    UUID l1 = UUID.randomUUID();
    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            sId,
            l1,
            "Lesson 1",
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            null,
            true);

    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1));

    when(enrollmentRepository.updateScheduleConfigAndVersion(
            eq(enrollment.getId()), eq(2L), eq(3L), anyString(), eq(45), any()))
        .thenReturn(1);

    ScheduleCommitRequest request =
        new ScheduleCommitRequest(
            LocalDate.of(2026, 10, 27), // shifted to Tuesday
            List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            45,
            2L);

    EnrollmentResponse response = service.commitReschedule(rawKey, enrollmentId, request);

    assertThat(response.version()).isEqualTo(3L);
    assertThat(response.studyDays()).containsExactly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
    verify(lessonScheduleRepository)
        .updatePlannedDate(eq(sId), eq(LocalDate.of(2026, 10, 27)), any());
    verify(idempotencyService)
        .record(eq(userId), eq("COMMIT_RESCHEDULE"), eq(rawKey), any(), eq(200), any());
  }

  @Test
  @DisplayName(
      "previewReschedule: handles uncompleted schedules containing optional lessons without error")
  void previewReschedule_handlesOptionalLessonsGracefully() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID l1 = UUID.randomUUID();
    UUID lOpt = UUID.randomUUID();

    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            l1,
            "Lesson 1",
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            null,
            true);
    LessonScheduleDetailDto sOpt =
        new LessonScheduleDetailDto(
            UUID.randomUUID(),
            lOpt,
            "Optional Lesson",
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            null,
            false);

    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1, sOpt));

    SchedulePreviewRequest request =
        new SchedulePreviewRequest(LocalDate.of(2026, 10, 27), List.of(DayOfWeek.TUESDAY), 30);

    SchedulePreviewResponse preview = service.previewReschedule(enrollmentId, request);

    assertThat(preview.enrollmentVersion()).isEqualTo(1L);
    assertThat(preview.proposedEndDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    assertThat(preview.modifiedSchedules()).hasSize(1);
    assertThat(preview.modifiedSchedules().get(0).lessonId()).isEqualTo(l1);
  }

  @Test
  @DisplayName(
      "pauseEnrollment: successfully transitions ACTIVE to PAUSED and increments version via CAS")
  void pauseEnrollment_success() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 3L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(enrollmentRepository.updateStatusAndVersion(
            eq(enrollmentId),
            eq(3L),
            eq(4L),
            eq(EnrollmentStatus.ACTIVE),
            eq(EnrollmentStatus.PAUSED),
            any()))
        .thenReturn(1);

    EnrollmentResponse response =
        service.pauseEnrollment(enrollmentId, new LifecycleTransitionRequest(3L));

    assertThat(response.status()).isEqualTo(EnrollmentStatus.PAUSED);
    assertThat(response.version()).isEqualTo(4L);
    verify(enrollmentRepository)
        .updateStatusAndVersion(
            eq(enrollmentId),
            eq(3L),
            eq(4L),
            eq(EnrollmentStatus.ACTIVE),
            eq(EnrollmentStatus.PAUSED),
            any());
  }

  @Test
  @DisplayName("pauseEnrollment: rejects non-ACTIVE enrollment with STATE_CONFLICT")
  void pauseEnrollment_rejectsNonActive() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.PAUSED, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(
            () -> service.pauseEnrollment(enrollmentId, new LifecycleTransitionRequest(1L)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("pauseEnrollment: rejects stale expectedVersion with VERSION_CONFLICT")
  void pauseEnrollment_rejectsStaleVersion() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 2L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(
            () -> service.pauseEnrollment(enrollmentId, new LifecycleTransitionRequest(1L)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERSION_CONFLICT);
  }

  @Test
  @DisplayName(
      "resumeEnrollment: successfully transitions PAUSED to ACTIVE, shifts uncompleted schedules in same tx")
  void resumeEnrollment_success() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.findExisting(userId, "RESUME_ENROLLMENT", rawKey))
        .thenReturn(Optional.empty());

    Enrollment enrollment = createEnrollment(EnrollmentStatus.PAUSED, 4L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID sId = UUID.randomUUID();
    UUID l1 = UUID.randomUUID();
    LessonScheduleDetailDto s1 =
        new LessonScheduleDetailDto(
            sId,
            l1,
            "Lesson 1",
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            null,
            true);
    when(lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId))
        .thenReturn(List.of(s1));

    when(enrollmentRepository.resumeEnrollment(
            eq(enrollmentId),
            eq(4L),
            eq(5L),
            eq(EnrollmentStatus.PAUSED),
            eq(EnrollmentStatus.ACTIVE),
            anyString(),
            eq(45),
            any()))
        .thenReturn(1);

    ScheduleCommitRequest request =
        new ScheduleCommitRequest(
            LocalDate.of(2026, 10, 27), List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 45, 4L);

    EnrollmentResponse response = service.resumeEnrollment(rawKey, enrollmentId, request);

    assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(response.version()).isEqualTo(5L);
    assertThat(response.studyDays()).containsExactly(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
    verify(lessonScheduleRepository)
        .updatePlannedDate(eq(sId), eq(LocalDate.of(2026, 10, 27)), any());
    verify(idempotencyService)
        .record(eq(userId), eq("RESUME_ENROLLMENT"), eq(rawKey), any(), eq(200), any());
  }

  @Test
  @DisplayName("resumeEnrollment: rejects non-PAUSED enrollment with STATE_CONFLICT")
  void resumeEnrollment_rejectsNonPaused() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.findExisting(userId, "RESUME_ENROLLMENT", rawKey))
        .thenReturn(Optional.empty());

    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE, 1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    ScheduleCommitRequest request =
        new ScheduleCommitRequest(LocalDate.of(2026, 10, 27), List.of(DayOfWeek.TUESDAY), 30, 1L);

    assertThatThrownBy(() -> service.resumeEnrollment(rawKey, enrollmentId, request))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("resumeEnrollment: replays idempotent request when key already recorded")
  void resumeEnrollment_replaysIdempotency() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("hashed-payload");
    IdempotencyRecord record = mock(IdempotencyRecord.class);
    when(idempotencyService.findExisting(userId, "RESUME_ENROLLMENT", rawKey))
        .thenReturn(Optional.of(record));

    EnrollmentResponse cached =
        new EnrollmentResponse(
            enrollmentId,
            userId,
            curriculumId,
            revisionId,
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            5L);
    when(idempotencyService.replay(eq(record), any(), eq(EnrollmentResponse.class)))
        .thenReturn(cached);

    ScheduleCommitRequest request =
        new ScheduleCommitRequest(LocalDate.of(2026, 10, 27), List.of(DayOfWeek.MONDAY), 30, 4L);

    EnrollmentResponse response = service.resumeEnrollment(rawKey, enrollmentId, request);
    assertThat(response.version()).isEqualTo(5L);
    verify(enrollmentRepository, never()).findById(any());
  }

  @Test
  @DisplayName("abandonEnrollment: transitions ACTIVE or PAUSED to ABANDONED")
  void abandonEnrollment_success() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.PAUSED, 2L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(enrollmentRepository.abandonEnrollment(
            eq(enrollmentId), eq(2L), eq(3L), any(), eq(EnrollmentStatus.ABANDONED), any()))
        .thenReturn(1);

    EnrollmentResponse response =
        service.abandonEnrollment(enrollmentId, new LifecycleTransitionRequest(2L));

    assertThat(response.status()).isEqualTo(EnrollmentStatus.ABANDONED);
    assertThat(response.version()).isEqualTo(3L);
  }

  @Test
  @DisplayName("abandonEnrollment: rejects COMPLETED or ABANDONED with STATE_CONFLICT")
  void abandonEnrollment_rejectsCompleted() {
    Enrollment enrollment = createEnrollment(EnrollmentStatus.COMPLETED, 5L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(
            () -> service.abandonEnrollment(enrollmentId, new LifecycleTransitionRequest(5L)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
  }
}
