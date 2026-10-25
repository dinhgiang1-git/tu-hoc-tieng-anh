package vn.englishlearning.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.englishlearning.curriculum.domain.Curriculum;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.api.EnrollmentRequests;
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.application.IdempotencyService;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.security.CurrentUser;

class CreateEnrollmentServiceTest {

  private EnrollmentRepository enrollmentRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private CurriculumRevisionRepository revisionRepository;
  private LessonRepository lessonRepository;
  private LearnerPreferencesRepository preferencesRepository;
  private AppUserRepository userRepository;
  private IdempotencyService idempotencyService;
  private CurrentUser currentUser;
  private Clock clock;
  private CreateEnrollmentService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final Instant fixedNow = Instant.parse("2026-10-25T10:00:00Z");

  @BeforeEach
  void setUp() {
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    revisionRepository = mock(CurriculumRevisionRepository.class);
    lessonRepository = mock(LessonRepository.class);
    preferencesRepository = mock(LearnerPreferencesRepository.class);
    userRepository = mock(AppUserRepository.class);
    idempotencyService = mock(IdempotencyService.class);
    currentUser = mock(CurrentUser.class);
    clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    when(currentUser.requireUserId()).thenReturn(userId);
    when(userRepository.findByIdForUpdate(userId))
        .thenReturn(
            Optional.of(
                new AppUser(
                    "testuser",
                    "hash",
                    "Test User",
                    "test@example.com",
                    UserRole.LEARNER,
                    fixedNow)));

    service =
        new CreateEnrollmentService(
            enrollmentRepository,
            lessonScheduleRepository,
            revisionRepository,
            lessonRepository,
            preferencesRepository,
            userRepository,
            idempotencyService,
            currentUser,
            clock);
  }

  @Test
  @DisplayName(
      "Generates initial schedule correctly for Mon/Wed/Fri starting 2026-10-26 with 5 required lessons")
  void createEnrollment_standardMonWedFriSchedule() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("payload-hash");
    when(idempotencyService.findExisting(eq(userId), eq("CREATE_ENROLLMENT"), eq(rawKey)))
        .thenReturn(Optional.empty());

    Curriculum curriculum = new Curriculum("pilot", "en-GB", fixedNow);
    org.springframework.test.util.ReflectionTestUtils.setField(curriculum, "id", curriculumId);
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum, "1.0.0", "Title", "Desc", "Authors", "Prov", "hash", fixedNow);
    org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);
    revision.publish(fixedNow);

    when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
    when(enrollmentRepository.existsByUserIdAndCurriculumIdAndStatusIn(
            eq(userId), eq(curriculum.getId()), any()))
        .thenReturn(false);
    when(enrollmentRepository.save(any(Enrollment.class)))
        .thenAnswer(
            invocation -> {
              Enrollment e = invocation.getArgument(0);
              org.springframework.test.util.ReflectionTestUtils.setField(
                  e, "id", UUID.randomUUID());
              return e;
            });

    CurriculumPhase phase = new CurriculumPhase(revision, "p1", "Phase 1", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "w1", 1, 1, "Week 1", "Goal");

    List<Lesson> lessons = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Lesson l = new Lesson(week, "lesson-" + i, i, "Lesson " + i, 20, true, "Content");
      org.springframework.test.util.ReflectionTestUtils.setField(l, "id", UUID.randomUUID());
      lessons.add(l);
    }
    when(lessonRepository.findRequiredLessonsByRevisionIdOrdered(revisionId)).thenReturn(lessons);

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculum.getId(),
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    EnrollmentResponses.EnrollmentResponse response = service.createEnrollment(rawKey, request);

    assertThat(response).isNotNull();
    assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 10, 26));

    // Verify 5 schedules captured
    ArgumentCaptor<List<LessonSchedule>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
    verify(lessonScheduleRepository).saveAll(scheduleCaptor.capture());
    List<LessonSchedule> savedSchedules = scheduleCaptor.getValue();
    assertThat(savedSchedules).hasSize(5);

    assertThat(savedSchedules.get(0).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 26)); // Mon
    assertThat(savedSchedules.get(1).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(savedSchedules.get(2).getPlannedDate()).isEqualTo(LocalDate.of(2026, 10, 30)); // Fri
    assertThat(savedSchedules.get(3).getPlannedDate()).isEqualTo(LocalDate.of(2026, 11, 2)); // Mon
    assertThat(savedSchedules.get(4).getPlannedDate()).isEqualTo(LocalDate.of(2026, 11, 4)); // Wed

    // Verify preferences updated with focusedEnrollmentId
    verify(preferencesRepository).save(any(LearnerPreferences.class));
    // Verify idempotency record saved
    verify(idempotencyService)
        .record(
            eq(userId),
            eq("CREATE_ENROLLMENT"),
            eq(rawKey),
            eq("payload-hash"),
            eq(201),
            any(EnrollmentResponses.EnrollmentResponse.class));
  }

  @Test
  @DisplayName("Replays cached response on duplicate idempotent request")
  void createEnrollment_idempotentReplay() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("payload-hash");

    IdempotencyRecord record =
        new IdempotencyRecord(
            userId, "CREATE_ENROLLMENT", rawKey, "payload-hash", 201, "{}", fixedNow);
    when(idempotencyService.findExisting(eq(userId), eq("CREATE_ENROLLMENT"), eq(rawKey)))
        .thenReturn(Optional.of(record));

    EnrollmentResponses.EnrollmentResponse cached =
        new EnrollmentResponses.EnrollmentResponse(
            UUID.randomUUID(),
            userId,
            curriculumId,
            revisionId,
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            1L);

    when(idempotencyService.replay(
            eq(record), eq("payload-hash"), eq(EnrollmentResponses.EnrollmentResponse.class)))
        .thenReturn(cached);

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY),
            30,
            "Asia/Ho_Chi_Minh");

    EnrollmentResponses.EnrollmentResponse result = service.createEnrollment(rawKey, request);

    assertThat(result).isSameAs(cached);
    verify(enrollmentRepository, never()).save(any());
    verify(lessonScheduleRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("Throws STATE_CONFLICT when active enrollment already exists for curriculum")
  void createEnrollment_existingActiveEnrollment_throwsStateConflict() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("payload-hash");
    when(idempotencyService.findExisting(eq(userId), eq("CREATE_ENROLLMENT"), eq(rawKey)))
        .thenReturn(Optional.empty());

    when(enrollmentRepository.existsByUserIdAndCurriculumIdAndStatusIn(
            eq(userId), eq(curriculumId), any()))
        .thenReturn(true);

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY),
            30,
            "Asia/Ho_Chi_Minh");

    assertThatThrownBy(() -> service.createEnrollment(rawKey, request))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
  }

  @Test
  @DisplayName("Throws VALIDATION_FAILED when minutes or study days or timezone are invalid")
  void createEnrollment_validationErrors() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);

    // minutes < 10
    assertThatThrownBy(
            () ->
                service.createEnrollment(
                    rawKey,
                    new EnrollmentRequests.CreateEnrollmentRequest(
                        curriculumId,
                        revisionId,
                        LocalDate.now(),
                        List.of(DayOfWeek.MONDAY),
                        5,
                        "UTC")))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

    // empty studyDays
    assertThatThrownBy(
            () ->
                service.createEnrollment(
                    rawKey,
                    new EnrollmentRequests.CreateEnrollmentRequest(
                        curriculumId,
                        revisionId,
                        LocalDate.now(),
                        Collections.emptyList(),
                        30,
                        "UTC")))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

    // invalid timezone
    assertThatThrownBy(
            () ->
                service.createEnrollment(
                    rawKey,
                    new EnrollmentRequests.CreateEnrollmentRequest(
                        curriculumId,
                        revisionId,
                        LocalDate.now(),
                        List.of(DayOfWeek.MONDAY),
                        30,
                        "Invalid/Timezone")))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("Throws STATE_CONFLICT when attempting to enroll into non-published revision")
  void createEnrollment_nonPublishedRevision_throwsStateConflict() {
    String rawKey = UUID.randomUUID().toString();
    when(idempotencyService.validateAndNormalizeKey(rawKey)).thenReturn(rawKey);
    when(idempotencyService.hashPayload(anyString())).thenReturn("payload-hash");
    when(idempotencyService.findExisting(eq(userId), eq("CREATE_ENROLLMENT"), eq(rawKey)))
        .thenReturn(Optional.empty());

    Curriculum curriculum = new Curriculum("pilot", "en-GB", fixedNow);
    org.springframework.test.util.ReflectionTestUtils.setField(curriculum, "id", curriculumId);
    CurriculumRevision draftRevision =
        new CurriculumRevision(
            curriculum, "1.0.0", "Title", "Desc", "Authors", "Prov", "hash", fixedNow);
    org.springframework.test.util.ReflectionTestUtils.setField(draftRevision, "id", revisionId);
    // status is DRAFT

    when(revisionRepository.findById(revisionId)).thenReturn(Optional.of(draftRevision));
    when(enrollmentRepository.existsByUserIdAndCurriculumIdAndStatusIn(
            eq(userId), eq(curriculum.getId()), any()))
        .thenReturn(false);

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculum.getId(),
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY),
            30,
            "Asia/Ho_Chi_Minh");

    assertThatThrownBy(() -> service.createEnrollment(rawKey, request))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STATE_CONFLICT);
  }
}
