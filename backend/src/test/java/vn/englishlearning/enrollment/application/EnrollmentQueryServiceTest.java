package vn.englishlearning.enrollment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleItemDto;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

class EnrollmentQueryServiceTest {

  private EnrollmentRepository enrollmentRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private CurriculumRevisionRepository curriculumRevisionRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private ActivityProgressRepository activityProgressRepository;
  private CurrentUser currentUser;
  private Clock clock;
  private EnrollmentQueryService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID otherUserId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-10-25T10:00:00Z");

  private final java.util.Map<UUID, RevisionInfoDto> revisionStorage = new java.util.HashMap<>();

  @BeforeEach
  void setUp() {
    enrollmentRepository = mock(EnrollmentRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    curriculumRevisionRepository = mock(CurriculumRevisionRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    activityProgressRepository = mock(ActivityProgressRepository.class);
    currentUser = mock(CurrentUser.class);
    clock = Clock.fixed(now, ZoneId.of("UTC"));

    when(currentUser.requireUserId()).thenReturn(userId);

    revisionStorage.clear();
    when(curriculumRevisionRepository.findRevisionInfosByIds(any()))
        .thenAnswer(
            inv -> {
              Collection<UUID> ids = inv.getArgument(0);
              return ids.stream().map(revisionStorage::get).filter(Objects::nonNull).toList();
            });
    when(curriculumRevisionRepository.findRevisionInfoById(any()))
        .thenAnswer(inv -> Optional.ofNullable(revisionStorage.get(inv.getArgument(0))));
    when(lessonRepository.findLessonSummariesByRevisionIds(any()))
        .thenReturn(java.util.Collections.emptyList());
    when(activityRepository.findActivitySummariesByRevisionIds(any()))
        .thenReturn(java.util.Collections.emptyList());
    when(activityProgressRepository.findProgressDtosByEnrollmentIds(any()))
        .thenReturn(java.util.Collections.emptyList());

    service =
        new EnrollmentQueryService(
            enrollmentRepository,
            lessonScheduleRepository,
            curriculumRevisionRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            currentUser,
            clock);
  }

  @Test
  @DisplayName("getDetail returns enrollment detail when requested by owner")
  void getDetail_owner_returnsDetail() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            now);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
    when(lessonScheduleRepository.findMaxPlannedDateByEnrollmentId(enrollmentId))
        .thenReturn(Optional.of(LocalDate.of(2026, 11, 20)));

    EnrollmentResponses.EnrollmentDetailResponse response = service.getDetail(enrollmentId);

    assertThat(response).isNotNull();
    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(response.studyDays())
        .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    assertThat(response.dailyTargetMinutes()).isEqualTo(30);
    assertThat(response.estimatedEndDate()).isEqualTo(LocalDate.of(2026, 11, 20));
  }

  @Test
  @DisplayName("getDetail throws RESOURCE_NOT_FOUND when requested by different user")
  void getDetail_differentUser_throwsNotFound() {
    Enrollment enrollment =
        new Enrollment(
            otherUserId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);

    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(() -> service.getDetail(enrollmentId))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("getMyEnrollments computes progress using effectively required activities")
  void getMyEnrollments_computesProgress() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(enrollment, "id", enrollmentId);

    when(enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(enrollment));

    Curriculum curriculum = new Curriculum("pilot", "en-GB", now);
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum, "1.0.0", "Pilot Course", "Desc", "Auth", "Prov", "hash", now);
    when(curriculumRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

    CurriculumPhase phase = new CurriculumPhase(revision, "p1", "Phase", 1);
    CurriculumWeek week = new CurriculumWeek(phase, "w1", 1, 1, "Week", "Goal");
    Lesson lesson1 = new Lesson(week, "l1", 1, "Lesson 1", 20, true, "Content");
    UUID lesson1Id = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(lesson1, "id", lesson1Id);

    revisionStorage.put(revisionId, new RevisionInfoDto(revisionId, "Pilot Course", "1.0.0"));
    when(lessonRepository.findLessonSummariesByRevisionIds(any()))
        .thenReturn(
            List.of(
                new LessonSummaryDto(
                    lesson1Id, revisionId, week.getId(), 1, "Lesson 1", 20, true)));

    Activity act1 =
        new Activity(
            lesson1,
            "a1",
            1,
            "A1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Read",
            null);
    UUID act1Id = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(act1, "id", act1Id);

    Activity act2 =
        new Activity(
            lesson1,
            "a2",
            2,
            "A2",
            ActivityType.PRACTICE,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Practice",
            null);
    UUID act2Id = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(act2, "id", act2Id);

    when(activityRepository.findActivitySummariesByRevisionIds(any()))
        .thenReturn(
            List.of(
                new ActivitySummaryDto(act1Id, lesson1Id, 1, true),
                new ActivitySummaryDto(act2Id, lesson1Id, 2, true)));

    when(activityProgressRepository.findProgressDtosByEnrollmentIds(List.of(enrollment.getId())))
        .thenReturn(
            List.of(
                new ActivityProgressDto(
                    enrollment.getId(), act1.getId(), ActivityStatus.COMPLETED)));

    List<EnrollmentResponses.EnrollmentSummaryResponse> summaries = service.getMyEnrollments();

    assertThat(summaries).hasSize(1);
    EnrollmentResponses.EnrollmentSummaryResponse summary = summaries.get(0);
    assertThat(summary.curriculumTitle()).isEqualTo("Pilot Course");
    assertThat(summary.totalRequiredActivities()).isEqualTo(2);
    assertThat(summary.completedRequiredActivities()).isEqualTo(1);
    assertThat(summary.progressPercent()).isEqualByComparingTo(new BigDecimal("50.0"));
  }

  @Test
  @DisplayName("getLessonSummary returns lesson summary for owner")
  void getLessonSummary_owner_returnsSummary() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    Curriculum curriculum = new Curriculum("pilot", "en-GB", now);
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum, "1.0.0", "Pilot Course", "Desc", "Auth", "Prov", "hash", now);
    org.springframework.test.util.ReflectionTestUtils.setField(revision, "id", revisionId);
    revisionStorage.put(revisionId, new RevisionInfoDto(revisionId, "Pilot Course", "1.0.0"));
    when(curriculumRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

    UUID lessonId = UUID.randomUUID();
    UUID weekId = UUID.randomUUID();
    LessonSummaryDto lessonDto =
        new LessonSummaryDto(lessonId, revisionId, weekId, 1, "Lesson 1", 25, true);
    when(lessonRepository.findLessonSummaryById(lessonId)).thenReturn(Optional.of(lessonDto));

    LessonSchedule schedule =
        new LessonSchedule(
            enrollmentId, lessonId, LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 26), now);
    when(lessonScheduleRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId))
        .thenReturn(Optional.of(schedule));

    EnrollmentResponses.LessonSummaryResponse result =
        service.getLessonSummary(enrollmentId, lessonId);

    assertThat(result.enrollmentId()).isEqualTo(enrollmentId);
    assertThat(result.lessonId()).isEqualTo(lessonId);
    assertThat(result.curriculumTitle()).isEqualTo("Pilot Course");
    assertThat(result.lessonTitle()).isEqualTo("Lesson 1");
    assertThat(result.estimatedMinutes()).isEqualTo(25);
    assertThat(result.plannedDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(result.isRequired()).isTrue();
  }

  @Test
  @DisplayName("getLessonSummary throws 404 when lesson does not belong to enrollment revision")
  void getLessonSummary_lessonMismatch_throwsNotFound() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(enrollment, "id", enrollmentId);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    UUID otherRevId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    LessonSummaryDto otherLesson =
        new LessonSummaryDto(lessonId, otherRevId, UUID.randomUUID(), 1, "Other Lesson", 25, true);
    when(lessonRepository.findLessonSummaryById(lessonId)).thenReturn(Optional.of(otherLesson));

    assertThatThrownBy(() -> service.getLessonSummary(enrollmentId, lessonId))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("getLessonSummary throws 404 when unauthorized user accesses enrollment")
  void getLessonSummary_unauthorizedUser_throwsNotFound() {
    Enrollment otherUserEnrollment =
        new Enrollment(
            otherUserId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(otherUserEnrollment));

    assertThatThrownBy(() -> service.getLessonSummary(enrollmentId, UUID.randomUUID()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("R4: getMyEnrollments uses batch projections and does not call entity queries")
  void getMyEnrollments_usesBatchProjectionsWithoutEntityQueries() {
    Enrollment e1 =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(e1, "id", UUID.randomUUID());

    Enrollment e2 =
        new Enrollment(
            userId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.TUESDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(e2, "id", UUID.randomUUID());

    when(enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(e1, e2));

    List<EnrollmentResponses.EnrollmentSummaryResponse> result = service.getMyEnrollments();

    assertThat(result).hasSize(2);
    verify(activityProgressRepository, never()).findByEnrollmentId(any());
    verify(curriculumRevisionRepository, never()).findById(any());
    verify(lessonRepository, never()).findRequiredLessonsByRevisionIdOrdered(any());
    verify(activityRepository, never()).findByLessonWeekPhaseRevisionId(any());
  }

  @Test
  @DisplayName(
      "getSchedule with default range queries current week Monday to Sunday in enrollment timezone")
  void getSchedule_defaultRange_queriesCurrentWeekMondayToSunday() {
    // 2026-10-28 03:00 UTC = 2026-10-28 10:00 in Asia/Ho_Chi_Minh (Wednesday)
    Clock wednesdayClock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneId.of("UTC"));
    EnrollmentQueryService serviceWithClock =
        new EnrollmentQueryService(
            enrollmentRepository,
            lessonScheduleRepository,
            curriculumRevisionRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            currentUser,
            wednesdayClock);

    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate expectedMon = LocalDate.of(2026, 10, 26);
    LocalDate expectedSun = LocalDate.of(2026, 11, 1);

    UUID lessonId = UUID.randomUUID();
    when(lessonScheduleRepository.findScheduleItemsByEnrollmentIdAndRange(
            enrollmentId, expectedMon, expectedSun))
        .thenReturn(
            List.of(new LessonScheduleItemDto(lessonId, "Bài 1", expectedMon, expectedMon, null)));

    List<EnrollmentResponses.LessonScheduleResponse> result =
        serviceWithClock.getSchedule(enrollmentId, null, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).lessonId()).isEqualTo(lessonId);
    assertThat(result.get(0).lessonTitle()).isEqualTo("Bài 1");
    assertThat(result.get(0).plannedDate()).isEqualTo(expectedMon);
    verify(lessonScheduleRepository)
        .findScheduleItemsByEnrollmentIdAndRange(enrollmentId, expectedMon, expectedSun);
  }

  @Test
  @DisplayName("getSchedule with only from provided computes to = from + 6 days")
  void getSchedule_onlyFromProvided_toIsFromPlus6() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate from = LocalDate.of(2026, 11, 2);
    LocalDate expectedTo = LocalDate.of(2026, 11, 8);

    when(lessonScheduleRepository.findScheduleItemsByEnrollmentIdAndRange(
            enrollmentId, from, expectedTo))
        .thenReturn(List.of());

    List<EnrollmentResponses.LessonScheduleResponse> result =
        service.getSchedule(enrollmentId, from, null);

    assertThat(result).isEmpty();
    verify(lessonScheduleRepository)
        .findScheduleItemsByEnrollmentIdAndRange(enrollmentId, from, expectedTo);
  }

  @Test
  @DisplayName("getSchedule with only to provided computes from = to - 6 days")
  void getSchedule_onlyToProvided_fromIsToMinus6() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate to = LocalDate.of(2026, 11, 8);
    LocalDate expectedFrom = LocalDate.of(2026, 11, 2);

    when(lessonScheduleRepository.findScheduleItemsByEnrollmentIdAndRange(
            enrollmentId, expectedFrom, to))
        .thenReturn(List.of());

    List<EnrollmentResponses.LessonScheduleResponse> result =
        service.getSchedule(enrollmentId, null, to);

    assertThat(result).isEmpty();
    verify(lessonScheduleRepository)
        .findScheduleItemsByEnrollmentIdAndRange(enrollmentId, expectedFrom, to);
  }

  @Test
  @DisplayName("getSchedule throws VALIDATION_FAILED when from is after to")
  void getSchedule_fromAfterTo_throwsValidationFailed() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate from = LocalDate.of(2026, 11, 10);
    LocalDate to = LocalDate.of(2026, 11, 1);

    assertThatThrownBy(() -> service.getSchedule(enrollmentId, from, to))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("getSchedule throws VALIDATION_FAILED when range exceeds 366 days")
  void getSchedule_rangeExceeds366Days_throwsValidationFailed() {
    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2027, 1, 3); // 368 days

    assertThatThrownBy(() -> service.getSchedule(enrollmentId, from, to))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("getSchedule throws RESOURCE_NOT_FOUND when requested by non-owner")
  void getSchedule_otherUser_throwsNotFound() {
    Enrollment enrollment =
        new Enrollment(
            otherUserId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(() -> service.getSchedule(enrollmentId, null, null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("getSchedule calculates isOverdue correctly for ACTIVE enrollment")
  void getSchedule_activeEnrollment_calculatesOverdueCorrectly() {
    // 2026-10-28 03:00 UTC = 2026-10-28 in Asia/Ho_Chi_Minh (Wednesday)
    Clock clock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneId.of("UTC"));
    EnrollmentQueryService customService =
        new EnrollmentQueryService(
            enrollmentRepository,
            lessonScheduleRepository,
            curriculumRevisionRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            currentUser,
            clock);

    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            now);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate past = LocalDate.of(2026, 10, 26);
    LocalDate today = LocalDate.of(2026, 10, 28);
    LocalDate future = LocalDate.of(2026, 10, 30);

    UUID l1 = UUID.randomUUID();
    UUID l2 = UUID.randomUUID();
    UUID l3 = UUID.randomUUID();
    UUID l4 = UUID.randomUUID();

    when(lessonScheduleRepository.findScheduleItemsByEnrollmentIdAndRange(
            eq(enrollmentId), any(), any()))
        .thenReturn(
            List.of(
                // past and uncompleted -> OVERDUE
                new LessonScheduleItemDto(l1, "Past Uncompleted", past, past, null),
                // past and completed -> NOT overdue
                new LessonScheduleItemDto(l2, "Past Completed", past, past, Instant.now()),
                // today and uncompleted -> NOT overdue
                new LessonScheduleItemDto(l3, "Today Uncompleted", today, today, null),
                // future and uncompleted -> NOT overdue
                new LessonScheduleItemDto(l4, "Future Uncompleted", future, future, null)));

    List<EnrollmentResponses.LessonScheduleResponse> result =
        customService.getSchedule(enrollmentId, past, future);

    assertThat(result).hasSize(4);
    // l1: past uncompleted
    assertThat(result.get(0).isCompleted()).isFalse();
    assertThat(result.get(0).isOverdue()).isTrue();
    // l2: past completed
    assertThat(result.get(1).isCompleted()).isTrue();
    assertThat(result.get(1).isOverdue()).isFalse();
    // l3: today uncompleted
    assertThat(result.get(2).isCompleted()).isFalse();
    assertThat(result.get(2).isOverdue()).isFalse();
    // l4: future uncompleted
    assertThat(result.get(3).isCompleted()).isFalse();
    assertThat(result.get(3).isOverdue()).isFalse();
  }

  @Test
  @DisplayName("getSchedule for PAUSED enrollment never flags lessons as overdue")
  void getSchedule_pausedEnrollment_neverOverdue() {
    Clock clock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneId.of("UTC"));
    EnrollmentQueryService customService =
        new EnrollmentQueryService(
            enrollmentRepository,
            lessonScheduleRepository,
            curriculumRevisionRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            currentUser,
            clock);

    Enrollment enrollment =
        new Enrollment(
            userId,
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            now);
    org.springframework.test.util.ReflectionTestUtils.setField(
        enrollment, "status", EnrollmentStatus.PAUSED);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    LocalDate past = LocalDate.of(2026, 10, 26);
    UUID l1 = UUID.randomUUID();

    when(lessonScheduleRepository.findScheduleItemsByEnrollmentIdAndRange(
            eq(enrollmentId), any(), any()))
        .thenReturn(List.of(new LessonScheduleItemDto(l1, "Past Uncompleted", past, past, null)));

    List<EnrollmentResponses.LessonScheduleResponse> result =
        customService.getSchedule(enrollmentId, past, past);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isCompleted()).isFalse();
    assertThat(result.get(0).isOverdue()).isFalse();
  }
}
