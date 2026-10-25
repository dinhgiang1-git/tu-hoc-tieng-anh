package vn.englishlearning.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.Curriculum;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.LessonSummaryDto;
import vn.englishlearning.curriculum.infrastructure.RevisionInfoDto;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.learning.api.TodayResponses;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

class TodayQueryServiceTest {

  private EnrollmentRepository enrollmentRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private ActivityProgressRepository activityProgressRepository;
  private CurriculumRevisionRepository curriculumRevisionRepository;
  private LearnerPreferencesRepository learnerPreferencesRepository;
  private CurrentUser currentUser;

  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();

  // Test setup helper for curriculum & lessons
  private CurriculumRevision revision;
  private Lesson lesson1;
  private Lesson lesson2;
  private Lesson lesson3;
  private Activity act1_1;
  private Activity act1_2;
  private Activity act2_1;
  private Activity act2_2;
  private Activity act3_1;

  private final java.util.Map<UUID, List<ActivityProgressDto>> progressStorage =
      new java.util.HashMap<>();

  private void mockProgress(UUID enrollmentId, List<ActivityProgress> progresses) {
    progressStorage.put(
        enrollmentId,
        progresses.stream()
            .map(
                p -> new ActivityProgressDto(p.getEnrollmentId(), p.getActivityId(), p.getStatus()))
            .toList());
  }

  @BeforeEach
  void setUp() {
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    activityProgressRepository = mock(ActivityProgressRepository.class);
    curriculumRevisionRepository = mock(CurriculumRevisionRepository.class);
    learnerPreferencesRepository = mock(LearnerPreferencesRepository.class);
    currentUser = mock(CurrentUser.class);

    when(currentUser.requireUserId()).thenReturn(userId);

    Instant now = Instant.parse("2026-10-25T10:00:00Z");
    Curriculum curriculum = new Curriculum("pilot", "en-GB", now);
    revision =
        new CurriculumRevision(
            curriculum,
            "1.0.0",
            "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
            "Desc",
            "Auth",
            "Prov",
            "hash",
            now);

    when(curriculumRevisionRepository.findRevisionInfoById(revisionId))
        .thenAnswer(
            inv ->
                Optional.of(
                    new RevisionInfoDto(revisionId, revision.getTitle(), revision.getVersion())));
    when(curriculumRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));
    org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);

    CurriculumPhase phase = new CurriculumPhase(revision, "p1", "Phase 1", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "w1", 1, 1, "Week 1", "Goal");

    lesson1 = new Lesson(week, "lesson-1", 1, "Buổi 1: Giới thiệu", 15, true, "Content 1");
    lesson2 = new Lesson(week, "lesson-2", 2, "Buổi 2: Cặp /p/ và /b/", 20, true, "Content 2");
    lesson3 = new Lesson(week, "lesson-3", 3, "Buổi 3: Cặp /t/ và /d/", 20, true, "Content 3");

    org.springframework.test.util.ReflectionTestUtils.setField(lesson1, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(lesson2, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(lesson3, "id", UUID.randomUUID());

    act1_1 =
        new Activity(
            lesson1,
            "a1-1",
            1,
            "Act 1.1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Read",
            null);
    act1_2 =
        new Activity(
            lesson1,
            "a1-2",
            2,
            "Act 1.2",
            ActivityType.PRACTICE,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Practice",
            null);

    act2_1 =
        new Activity(
            lesson2,
            "a2-1",
            1,
            "Act 2.1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Read",
            null);
    act2_2 =
        new Activity(
            lesson2,
            "a2-2",
            2,
            "Act 2.2",
            ActivityType.PRACTICE,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Practice",
            null);

    act3_1 =
        new Activity(
            lesson3,
            "a3-1",
            1,
            "Act 3.1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Read",
            null);

    org.springframework.test.util.ReflectionTestUtils.setField(act1_1, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(act1_2, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(act2_1, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(act2_2, "id", UUID.randomUUID());
    org.springframework.test.util.ReflectionTestUtils.setField(act3_1, "id", UUID.randomUUID());

    when(lessonRepository.findRequiredLessonsByRevisionIdOrdered(revisionId))
        .thenReturn(List.of(lesson1, lesson2, lesson3));
    when(lessonRepository.findLessonSummariesByRevisionId(revisionId))
        .thenAnswer(
            inv ->
                List.of(
                    new LessonSummaryDto(
                        lesson1.getId(),
                        revisionId,
                        lesson1.getWeek().getId(),
                        lesson1.getOrderIndex(),
                        lesson1.getTitle(),
                        lesson1.getEstimatedMinutes(),
                        lesson1.isRequired()),
                    new LessonSummaryDto(
                        lesson2.getId(),
                        revisionId,
                        lesson2.getWeek().getId(),
                        lesson2.getOrderIndex(),
                        lesson2.getTitle(),
                        lesson2.getEstimatedMinutes(),
                        lesson2.isRequired()),
                    new LessonSummaryDto(
                        lesson3.getId(),
                        revisionId,
                        lesson3.getWeek().getId(),
                        lesson3.getOrderIndex(),
                        lesson3.getTitle(),
                        lesson3.getEstimatedMinutes(),
                        lesson3.isRequired())));

    when(activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson1.getId()))
        .thenReturn(List.of(act1_1, act1_2));
    when(activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson2.getId()))
        .thenReturn(List.of(act2_1, act2_2));
    when(activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson3.getId()))
        .thenReturn(List.of(act3_1));
    when(activityRepository.findByLessonWeekPhaseRevisionId(revisionId))
        .thenAnswer(inv -> List.of(act1_1, act1_2, act2_1, act2_2, act3_1));
    when(activityRepository.findActivitySummariesByRevisionId(revisionId))
        .thenAnswer(
            inv ->
                List.of(
                    new ActivitySummaryDto(
                        act1_1.getId(),
                        lesson1.getId(),
                        act1_1.getOrderIndex(),
                        act1_1.isRequired()),
                    new ActivitySummaryDto(
                        act1_2.getId(),
                        lesson1.getId(),
                        act1_2.getOrderIndex(),
                        act1_2.isRequired()),
                    new ActivitySummaryDto(
                        act2_1.getId(),
                        lesson2.getId(),
                        act2_1.getOrderIndex(),
                        act2_1.isRequired()),
                    new ActivitySummaryDto(
                        act2_2.getId(),
                        lesson2.getId(),
                        act2_2.getOrderIndex(),
                        act2_2.isRequired()),
                    new ActivitySummaryDto(
                        act3_1.getId(),
                        lesson3.getId(),
                        act3_1.getOrderIndex(),
                        act3_1.isRequired())));

    progressStorage.clear();
    when(activityProgressRepository.findProgressDtosByEnrollmentId(any()))
        .thenAnswer(
            inv ->
                progressStorage.getOrDefault(
                    inv.getArgument(0), java.util.Collections.emptyList()));
  }

  private TodayQueryService createServiceWithTime(String isoInstant) {
    Clock clock = Clock.fixed(Instant.parse(isoInstant), ZoneId.of("UTC"));
    return new TodayQueryService(
        clock,
        enrollmentRepository,
        lessonScheduleRepository,
        lessonRepository,
        activityRepository,
        activityProgressRepository,
        curriculumRevisionRepository,
        learnerPreferencesRepository,
        currentUser);
  }

  private Enrollment createEnrollment(EnrollmentStatus status) {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            Instant.parse("2026-10-25T10:00:00Z"));
    org.springframework.test.util.ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    if (status == EnrollmentStatus.PAUSED) {
      enrollment.setStatus(EnrollmentStatus.PAUSED);
    } else if (status == EnrollmentStatus.COMPLETED) {
      enrollment.setStatus(EnrollmentStatus.COMPLETED);
      enrollment.setCompletedAt(Instant.parse("2026-10-25T10:00:00Z"));
    }
    return enrollment;
  }

  @Test
  @DisplayName("State 1: NO_ENROLLMENT when user has no focused enrollment")
  void getToday_noEnrollment() {
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    when(learnerPreferencesRepository.findById(userId)).thenReturn(Optional.empty());

    TodayResponses.TodayResponse response = service.getToday(null);

    assertThat(response.state()).isEqualTo("NO_ENROLLMENT");
    assertThat(response.enrollmentId()).isNull();
    assertThat(response.primaryAction()).isNull();
    assertThat(response.overdueLessons()).isEmpty();
    assertThat(response.progress()).isNull();
    assertThat(response.isRestDay()).isFalse();
    assertThat(response.todayDate()).isEqualTo(LocalDate.of(2026, 10, 28));
  }

  @Test
  @DisplayName("State 2: CURRICULUM_COMPLETED when all required activities are completed")
  void getToday_curriculumCompleted() {
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z"); // Wed
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    // All 5 activities completed
    mockProgress(
        enrollment.getId(),
        List.of(
            new ActivityProgress(enrollment.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(enrollment.getId(), act1_2.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(enrollment.getId(), act2_1.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(enrollment.getId(), act2_2.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(
                enrollment.getId(), act3_1.getId(), ActivityStatus.COMPLETED, now)));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("CURRICULUM_COMPLETED");
    assertThat(response.primaryAction()).isNotNull();
    assertThat(response.primaryAction().type()).isEqualTo("CURRICULUM_COMPLETED");
    assertThat(response.primaryAction().lessonId()).isNull();
    assertThat(response.overdueLessons()).isEmpty();
    assertThat(response.progress().completedRequiredActivities()).isEqualTo(5);
    assertThat(response.progress().totalRequiredActivities()).isEqualTo(5);
    assertThat(response.progress().progressPercent()).isEqualByComparingTo("100.0");
  }

  @Test
  @DisplayName("State 3: PAUSED when enrollment is paused")
  void getToday_paused() {
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.PAUSED);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("PAUSED");
    assertThat(response.primaryAction()).isNull();
    assertThat(response.overdueLessons()).isEmpty();
    assertThat(response.progress()).isNotNull();
  }

  @Test
  @DisplayName("State 4: IN_PROGRESS is prioritized over overdue and due_today")
  void getToday_inProgressPriority() {
    // Wednesday 2026-10-28 in Asia/Ho_Chi_Minh
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Schedules: Lesson 1 was due 2026-10-26 (overdue!), Lesson 2 is due 2026-10-28 (today)
    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    LessonSchedule sched2 =
        new LessonSchedule(
            enrollment.getId(),
            lesson2.getId(),
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1, sched2));

    // Lesson 1 is IN_PROGRESS (1/2 completed)
    mockProgress(
        enrollment.getId(),
        List.of(
            new ActivityProgress(
                enrollment.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now)));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("IN_PROGRESS");
    assertThat(response.primaryAction().type()).isEqualTo("IN_PROGRESS");
    assertThat(response.primaryAction().lessonId()).isEqualTo(lesson1.getId());
    assertThat(response.primaryAction().completedActivities()).isEqualTo(1);
    assertThat(response.primaryAction().totalActivities()).isEqualTo(2);
  }

  @Test
  @DisplayName("State 5: OVERDUE when plannedDate < today and not started")
  void getToday_overdue() {
    // Wednesday 2026-10-28
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Schedules: Lesson 1 was due 2026-10-26 (overdue 2 days), Lesson 2 is due 2026-10-28 (today)
    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    LessonSchedule sched2 =
        new LessonSchedule(
            enrollment.getId(),
            lesson2.getId(),
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1, sched2));

    // No activity progress

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("OVERDUE");
    assertThat(response.primaryAction().type()).isEqualTo("OVERDUE");
    assertThat(response.primaryAction().lessonId()).isEqualTo(lesson1.getId());
  }

  @Test
  @DisplayName("State 6: DUE_TODAY when plannedDate == today and previous lessons are completed")
  void getToday_dueToday() {
    // Wednesday 2026-10-28
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    LessonSchedule sched2 =
        new LessonSchedule(
            enrollment.getId(),
            lesson2.getId(),
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1, sched2));

    // Lesson 1 is completed
    mockProgress(
        enrollment.getId(),
        List.of(
            new ActivityProgress(enrollment.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(
                enrollment.getId(), act1_2.getId(), ActivityStatus.COMPLETED, now)));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("DUE_TODAY");
    assertThat(response.primaryAction().type()).isEqualTo("DUE_TODAY");
    assertThat(response.primaryAction().lessonId()).isEqualTo(lesson2.getId());
    assertThat(response.isRestDay()).isFalse();
  }

  @Test
  @DisplayName("State 7: REST_DAY when today is rest day and no in-progress or overdue lessons")
  void getToday_restDay() {
    // Tuesday 2026-10-27 (rest day in Mon/Wed/Fri)
    TodayQueryService service = createServiceWithTime("2026-10-27T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Instant now = Instant.parse("2026-10-27T03:00:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    LessonSchedule sched2 =
        new LessonSchedule(
            enrollment.getId(),
            lesson2.getId(),
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 10, 28),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1, sched2));

    // Lesson 1 completed on Monday
    mockProgress(
        enrollment.getId(),
        List.of(
            new ActivityProgress(enrollment.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(
                enrollment.getId(), act1_2.getId(), ActivityStatus.COMPLETED, now)));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("REST_DAY");
    assertThat(response.isRestDay()).isTrue();
    assertThat(response.primaryAction()).isNotNull();
    assertThat(response.primaryAction().type()).isEqualTo("UPCOMING");
    assertThat(response.primaryAction().lessonId()).isEqualTo(lesson2.getId());
    assertThat(response.overdueLessons()).isEmpty();
  }

  @Test
  @DisplayName("State 8: UPCOMING when on study day before start date")
  void getToday_upcomingBeforeStartDate() {
    // Friday 2026-10-23 (a study day in Mon/Wed/Fri, but startDate is Monday 2026-10-26)
    TodayQueryService service = createServiceWithTime("2026-10-23T03:00:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Instant now = Instant.parse("2026-10-23T03:00:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.state()).isEqualTo("UPCOMING");
    assertThat(response.isRestDay()).isFalse();
    assertThat(response.primaryAction().type()).isEqualTo("UPCOMING");
    assertThat(response.primaryAction().lessonId()).isEqualTo(lesson1.getId());
    assertThat(response.overdueLessons()).isEmpty();
  }

  @Test
  @DisplayName("Timezone calculation: 23:30 in London is already next day in Ho Chi Minh")
  void getToday_timezoneAcrossMidnight() {
    // 2026-10-26T23:30:00Z UTC -> 2026-10-27T06:30 in Asia/Ho_Chi_Minh (Tuesday = Rest day)
    TodayQueryService service = createServiceWithTime("2026-10-26T23:30:00Z");
    Enrollment enrollment = createEnrollment(EnrollmentStatus.ACTIVE); // Asia/Ho_Chi_Minh
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Instant now = Instant.parse("2026-10-26T23:30:00Z");
    LessonSchedule sched1 =
        new LessonSchedule(
            enrollment.getId(),
            lesson1.getId(),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 26),
            now);
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId()))
        .thenReturn(List.of(sched1));

    // Lesson 1 is completed
    mockProgress(
        enrollment.getId(),
        List.of(
            new ActivityProgress(enrollment.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now),
            new ActivityProgress(
                enrollment.getId(), act1_2.getId(), ActivityStatus.COMPLETED, now)));

    TodayResponses.TodayResponse response = service.getToday(enrollmentId);

    assertThat(response.todayDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    assertThat(response.isRestDay()).isTrue();
    assertThat(response.state()).isEqualTo("REST_DAY");
  }

  @Test
  @DisplayName("Throws RESOURCE_NOT_FOUND when accessing enrollment of another user")
  void getToday_unauthorizedUser_throwsNotFound() {
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    Enrollment otherUserEnrollment =
        new Enrollment(
            otherUserId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            Instant.parse("2026-10-25T10:00:00Z"));

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(otherUserEnrollment));

    assertThatThrownBy(() -> service.getToday(enrollmentId))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName(
      "R1 Regression: Optional activity must not keep a completed required lesson in progress")
  void reviewOptionalActivityDoesNotKeepLessonOpen() {
    Enrollment e = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(e.getId())).thenReturn(Optional.of(e));
    org.springframework.test.util.ReflectionTestUtils.setField(act1_2, "isRequired", false);
    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    mockProgress(
        e.getId(),
        List.of(new ActivityProgress(e.getId(), act1_1.getId(), ActivityStatus.COMPLETED, now)));
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(e.getId()))
        .thenReturn(
            List.of(
                new LessonSchedule(
                    e.getId(),
                    lesson1.getId(),
                    LocalDate.of(2026, 10, 26),
                    LocalDate.of(2026, 10, 26),
                    now),
                new LessonSchedule(
                    e.getId(),
                    lesson2.getId(),
                    LocalDate.of(2026, 10, 28),
                    LocalDate.of(2026, 10, 28),
                    now)));
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    var result = service.getToday(e.getId());
    assertThat(result.state())
        .as("optional activity must not keep a completed required lesson in progress")
        .isEqualTo("DUE_TODAY");
    assertThat(result.primaryAction().lessonId()).isEqualTo(lesson2.getId());
  }

  @Test
  @DisplayName("R1 Regression: PAUSED enrollment must take priority over completed counts")
  void reviewPausedMustTakePriorityOverCompletedCounts() {
    Enrollment e = createEnrollment(EnrollmentStatus.PAUSED);
    when(enrollmentRepository.findById(e.getId())).thenReturn(Optional.of(e));
    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    var completed =
        List.of(act1_1, act1_2, act2_1, act2_2, act3_1).stream()
            .map(a -> new ActivityProgress(e.getId(), a.getId(), ActivityStatus.COMPLETED, now))
            .toList();
    mockProgress(e.getId(), completed);
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    assertThat(service.getToday(e.getId()).state()).isEqualTo("PAUSED");
  }

  @Test
  @DisplayName(
      "R4: Today does not fetch activities once per lesson and does not call entity queries")
  void reviewTodayDoesNotFetchActivitiesOncePerLesson() {
    Enrollment e = createEnrollment(EnrollmentStatus.ACTIVE);
    when(enrollmentRepository.findById(e.getId())).thenReturn(Optional.of(e));
    TodayQueryService service = createServiceWithTime("2026-10-28T03:00:00Z");
    service.getToday(e.getId());
    verify(activityRepository, never()).findByLessonIdOrderByOrderIndexAsc(any(UUID.class));
    verify(activityRepository, never()).findByLessonWeekPhaseRevisionId(any(UUID.class));
    verify(lessonRepository, never()).findRequiredLessonsByRevisionIdOrdered(any(UUID.class));
    verify(activityProgressRepository, never()).findByEnrollmentId(any(UUID.class));
  }
}
