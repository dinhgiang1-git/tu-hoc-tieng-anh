package vn.englishlearning.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.assessment.domain.QuizAttempt;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.infrastructure.ActivityProjectionDto;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.LessonSummaryDto;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.learning.api.AnalyticsResponses.EnrollmentAnalyticsResponse;
import vn.englishlearning.learning.api.AnalyticsResponses.WeeklyProgressItemDto;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.LearningEvent;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.learning.infrastructure.LearningEventRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

class LearningAnalyticsServiceTest {

  private CurrentUser currentUser;
  private EnrollmentRepository enrollmentRepository;
  private CurriculumRevisionRepository curriculumRevisionRepository;
  private CurriculumWeekRepository curriculumWeekRepository;
  private LessonRepository lessonRepository;
  private ActivityRepository activityRepository;
  private ActivityProgressRepository activityProgressRepository;
  private LessonScheduleRepository lessonScheduleRepository;
  private LearningEventRepository learningEventRepository;
  private QuizAttemptRepository quizAttemptRepository;
  private Clock clock;

  private LearningAnalyticsService analyticsService;

  private final UUID userId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID curriculumId = UUID.randomUUID();
  private final UUID revisionId = UUID.randomUUID();
  private final ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
  private final Instant fixedNow = Instant.parse("2026-10-28T10:00:00Z");

  @BeforeEach
  void setUp() {
    currentUser = mock(CurrentUser.class);
    enrollmentRepository = mock(EnrollmentRepository.class);
    curriculumRevisionRepository = mock(CurriculumRevisionRepository.class);
    curriculumWeekRepository = mock(CurriculumWeekRepository.class);
    lessonRepository = mock(LessonRepository.class);
    activityRepository = mock(ActivityRepository.class);
    activityProgressRepository = mock(ActivityProgressRepository.class);
    lessonScheduleRepository = mock(LessonScheduleRepository.class);
    learningEventRepository = mock(LearningEventRepository.class);
    quizAttemptRepository = mock(QuizAttemptRepository.class);
    clock = Clock.fixed(fixedNow, zoneId);

    analyticsService =
        new LearningAnalyticsService(
            currentUser,
            enrollmentRepository,
            curriculumRevisionRepository,
            curriculumWeekRepository,
            lessonRepository,
            activityRepository,
            activityProgressRepository,
            lessonScheduleRepository,
            learningEventRepository,
            quizAttemptRepository,
            clock);

    when(currentUser.requireUserId()).thenReturn(userId);
  }

  @Test
  @DisplayName("Should throw RESOURCE_NOT_FOUND when enrollment not found")
  void getAnalytics_enrollmentNotFound_throwsException() {
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> analyticsService.getAnalytics(enrollmentId))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("Should throw RESOURCE_NOT_FOUND when enrollment belongs to another user")
  void getAnalytics_ownershipMismatch_throwsException() {
    Enrollment otherEnrollment = mock(Enrollment.class);
    when(otherEnrollment.getUserId()).thenReturn(UUID.randomUUID());
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(otherEnrollment));

    assertThatThrownBy(() -> analyticsService.getAnalytics(enrollmentId))
        .isInstanceOf(DomainException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("Should compute analytics, weekly progress, streak, and quiz stats successfully")
  void getAnalytics_success() {
    // 1. Setup enrollment
    Enrollment enrollment = mock(Enrollment.class);
    when(enrollment.getId()).thenReturn(enrollmentId);
    when(enrollment.getUserId()).thenReturn(userId);
    when(enrollment.getCurriculumId()).thenReturn(curriculumId);
    when(enrollment.getRevisionId()).thenReturn(revisionId);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
    when(enrollment.getStartDate()).thenReturn(LocalDate.of(2026, 10, 1));
    when(enrollment.getTimeZone()).thenReturn("Asia/Ho_Chi_Minh");
    when(enrollment.getVersion()).thenReturn(5L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Curriculum Revision
    CurriculumRevision revision = mock(CurriculumRevision.class);
    when(revision.getTitle()).thenReturn("Lộ trình Tiếng Anh Giao Tiếp");
    when(curriculumRevisionRepository.findById(revisionId)).thenReturn(Optional.of(revision));

    // 2. Setup Weeks, Lessons, Activities
    UUID week1Id = UUID.randomUUID();
    UUID week2Id = UUID.randomUUID();
    CurriculumWeek week1 = mock(CurriculumWeek.class);
    when(week1.getId()).thenReturn(week1Id);
    when(week1.getWeekNumber()).thenReturn(1);
    when(week1.getTitle()).thenReturn("Tuần 1: Nhập môn");

    CurriculumWeek week2 = mock(CurriculumWeek.class);
    when(week2.getId()).thenReturn(week2Id);
    when(week2.getWeekNumber()).thenReturn(2);
    when(week2.getTitle()).thenReturn("Tuần 2: Từ vựng");

    when(curriculumWeekRepository.findWeeksByRevisionIdOrdered(revisionId))
        .thenReturn(List.of(week1, week2));

    UUID lesson1Id = UUID.randomUUID();
    UUID lesson2Id = UUID.randomUUID();
    UUID lesson3Id = UUID.randomUUID();

    LessonSummaryDto l1 =
        new LessonSummaryDto(lesson1Id, revisionId, week1Id, 1, "Bài 1", 30, true);
    LessonSummaryDto l2 =
        new LessonSummaryDto(lesson2Id, revisionId, week1Id, 2, "Bài 2", 45, true);
    LessonSummaryDto l3 =
        new LessonSummaryDto(lesson3Id, revisionId, week2Id, 1, "Bài 3", 60, true);
    when(lessonRepository.findLessonSummariesByRevisionId(revisionId))
        .thenReturn(List.of(l1, l2, l3));

    UUID act1Id = UUID.randomUUID();
    UUID act2Id = UUID.randomUUID();
    UUID act3Id = UUID.randomUUID();
    UUID act4Id = UUID.randomUUID();

    ActivityProjectionDto a1 =
        new ActivityProjectionDto(
            act1Id,
            lesson1Id,
            1,
            "Task 1",
            ActivityType.PRACTICE,
            true,
            CompletionPolicy.MANUAL_CHECK);
    ActivityProjectionDto a2 =
        new ActivityProjectionDto(
            act2Id,
            lesson1Id,
            2,
            "Task 2",
            ActivityType.REFLECTION,
            true,
            CompletionPolicy.RESPONSE_SUBMIT);
    ActivityProjectionDto a3 =
        new ActivityProjectionDto(
            act3Id, lesson2Id, 1, "Task 3", ActivityType.QUIZ, true, CompletionPolicy.ON_SUBMIT);

    ActivityProjectionDto a4 =
        new ActivityProjectionDto(
            act4Id,
            lesson3Id,
            1,
            "Task 4",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK);

    when(activityRepository.findActivityProjectionsByRevisionId(revisionId))
        .thenReturn(List.of(a1, a2, a3, a4));

    // 3. User Progress: act1, act2 completed (so lesson 1 is complete); act3 in progress
    ActivityProgress p1 = mock(ActivityProgress.class);
    when(p1.getActivityId()).thenReturn(act1Id);
    when(p1.getStatus()).thenReturn(ActivityStatus.COMPLETED);
    when(p1.getCompletedAt()).thenReturn(Instant.parse("2026-10-27T08:00:00Z"));

    ActivityProgress p2 = mock(ActivityProgress.class);
    when(p2.getActivityId()).thenReturn(act2Id);
    when(p2.getStatus()).thenReturn(ActivityStatus.COMPLETED);
    when(p2.getCompletedAt()).thenReturn(Instant.parse("2026-10-28T09:00:00Z"));

    when(activityProgressRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(p1, p2));

    // Schedules
    LessonSchedule s1 = mock(LessonSchedule.class);
    when(s1.getLessonId()).thenReturn(lesson1Id);
    when(s1.getCompletedAt()).thenReturn(Instant.parse("2026-10-28T09:00:00Z"));
    when(lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId))
        .thenReturn(List.of(s1));

    // 4. Learning Events (active on 2026-10-26, 2026-10-27, 2026-10-28) -> 3 days streak!
    LearningEvent e1 = mock(LearningEvent.class);
    when(e1.getId()).thenReturn(UUID.randomUUID());
    when(e1.getEventType()).thenReturn("COMPLETED");
    when(e1.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 26));
    when(e1.getOccurredAt()).thenReturn(Instant.parse("2026-10-26T07:00:00Z"));
    when(e1.getActivityId()).thenReturn(act1Id);

    LearningEvent e2 = mock(LearningEvent.class);
    when(e2.getId()).thenReturn(UUID.randomUUID());
    when(e2.getEventType()).thenReturn("COMPLETED");
    when(e2.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 27));
    when(e2.getOccurredAt()).thenReturn(Instant.parse("2026-10-27T08:00:00Z"));
    when(e2.getActivityId()).thenReturn(act1Id);

    LearningEvent e3 = mock(LearningEvent.class);
    when(e3.getId()).thenReturn(UUID.randomUUID());
    when(e3.getEventType()).thenReturn("QUIZ_SUBMITTED");
    when(e3.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 28));
    when(e3.getOccurredAt()).thenReturn(Instant.parse("2026-10-28T09:00:00Z"));
    when(e3.getActivityId()).thenReturn(act3Id);

    when(learningEventRepository.findByEnrollmentIdOrderByOccurredAtDesc(enrollmentId))
        .thenReturn(List.of(e3, e2, e1));

    // 5. Quiz Attempt
    QuizAttempt qa = mock(QuizAttempt.class);
    when(qa.isPassed()).thenReturn(true);
    when(qa.getScorePercentage()).thenReturn(90);
    when(quizAttemptRepository.findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(enrollmentId))
        .thenReturn(List.of(qa));

    // EXECUTE
    EnrollmentAnalyticsResponse response = analyticsService.getAnalytics(enrollmentId);

    // VERIFY Overall stats
    assertThat(response.enrollmentId()).isEqualTo(enrollmentId);
    assertThat(response.curriculumTitle()).isEqualTo("Lộ trình Tiếng Anh Giao Tiếp");
    assertThat(response.status()).isEqualTo("ACTIVE");
    assertThat(response.totalRequiredActivities()).isEqualTo(4);
    assertThat(response.completedRequiredActivities()).isEqualTo(2);
    assertThat(response.progressPercent()).isEqualByComparingTo(new BigDecimal("50.0"));
    assertThat(response.totalLessons()).isEqualTo(3);
    assertThat(response.completedLessons()).isEqualTo(1); // Lesson 1 completed
    assertThat(response.estimatedCompletedMinutes()).isEqualTo(30);
    assertThat(response.estimatedTotalMinutes()).isEqualTo(135); // 30 + 45 + 60

    // VERIFY Streak
    assertThat(response.streak().currentStreakDays()).isEqualTo(3);
    assertThat(response.streak().longestStreakDays()).isEqualTo(3);
    assertThat(response.streak().totalActiveDays()).isEqualTo(3);
    assertThat(response.streak().lastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 28));

    // VERIFY Weekly Progress
    assertThat(response.weeklyProgress()).hasSize(2);
    WeeklyProgressItemDto wp1 = response.weeklyProgress().get(0);
    assertThat(wp1.weekNumber()).isEqualTo(1);
    assertThat(wp1.totalRequiredActivities()).isEqualTo(3); // act1, act2, act3
    assertThat(wp1.completedRequiredActivities()).isEqualTo(2); // act1, act2
    assertThat(wp1.progressPercent()).isEqualByComparingTo(new BigDecimal("66.7"));
    assertThat(wp1.status()).isEqualTo("IN_PROGRESS");
    assertThat(wp1.completedLessons()).isEqualTo(1);

    WeeklyProgressItemDto wp2 = response.weeklyProgress().get(1);
    assertThat(wp2.weekNumber()).isEqualTo(2);
    assertThat(wp2.totalRequiredActivities()).isEqualTo(1);
    assertThat(wp2.completedRequiredActivities()).isEqualTo(0);
    assertThat(wp2.status()).isEqualTo("NOT_STARTED");

    // VERIFY Quiz summary
    assertThat(response.quizSummary()).isNotNull();
    assertThat(response.quizSummary().totalQuizzesTaken()).isEqualTo(1);
    assertThat(response.quizSummary().passedQuizzes()).isEqualTo(1);
    assertThat(response.quizSummary().averageScorePercent())
        .isEqualByComparingTo(new BigDecimal("90.0"));

    // VERIFY Daily History
    assertThat(response.dailyHistory()).hasSize(3);
    assertThat(response.dailyHistory().get(0).date()).isEqualTo(LocalDate.of(2026, 10, 28));
    assertThat(response.dailyHistory().get(0).quizAttemptsCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should maintain streak when today has no activity but yesterday was active")
  void getAnalytics_streakMaintainedFromYesterday() {
    Enrollment enrollment = mock(Enrollment.class);
    when(enrollment.getId()).thenReturn(enrollmentId);
    when(enrollment.getUserId()).thenReturn(userId);
    when(enrollment.getCurriculumId()).thenReturn(curriculumId);
    when(enrollment.getRevisionId()).thenReturn(revisionId);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
    when(enrollment.getStartDate()).thenReturn(LocalDate.of(2026, 10, 1));
    when(enrollment.getTimeZone()).thenReturn("Asia/Ho_Chi_Minh");
    when(enrollment.getVersion()).thenReturn(1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Active yesterday: 2026-10-27 and 2026-10-26 (today is 2026-10-28)
    LearningEvent e1 = mock(LearningEvent.class);
    when(e1.getId()).thenReturn(UUID.randomUUID());
    when(e1.getEventType()).thenReturn("COMPLETED");
    when(e1.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 26));
    when(e1.getOccurredAt()).thenReturn(Instant.parse("2026-10-26T07:00:00Z"));

    LearningEvent e2 = mock(LearningEvent.class);
    when(e2.getId()).thenReturn(UUID.randomUUID());
    when(e2.getEventType()).thenReturn("COMPLETED");
    when(e2.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 27));
    when(e2.getOccurredAt()).thenReturn(Instant.parse("2026-10-27T08:00:00Z"));

    when(learningEventRepository.findByEnrollmentIdOrderByOccurredAtDesc(enrollmentId))
        .thenReturn(List.of(e2, e1));

    EnrollmentAnalyticsResponse response = analyticsService.getAnalytics(enrollmentId);

    // Current streak should be 2 because yesterday was active
    assertThat(response.streak().currentStreakDays()).isEqualTo(2);
    assertThat(response.streak().longestStreakDays()).isEqualTo(2);
    assertThat(response.streak().lastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 27));
  }

  @Test
  @DisplayName("Should reset current streak to 0 when neither today nor yesterday had activity")
  void getAnalytics_streakZeroWhenGap() {
    Enrollment enrollment = mock(Enrollment.class);
    when(enrollment.getId()).thenReturn(enrollmentId);
    when(enrollment.getUserId()).thenReturn(userId);
    when(enrollment.getCurriculumId()).thenReturn(curriculumId);
    when(enrollment.getRevisionId()).thenReturn(revisionId);
    when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
    when(enrollment.getStartDate()).thenReturn(LocalDate.of(2026, 10, 1));
    when(enrollment.getTimeZone()).thenReturn("Asia/Ho_Chi_Minh");
    when(enrollment.getVersion()).thenReturn(1L);
    when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

    // Active 4 days ago: 2026-10-24 and 2026-10-25 (today is 2026-10-28)
    LearningEvent e1 = mock(LearningEvent.class);
    when(e1.getId()).thenReturn(UUID.randomUUID());
    when(e1.getEventType()).thenReturn("COMPLETED");
    when(e1.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 24));
    when(e1.getOccurredAt()).thenReturn(Instant.parse("2026-10-24T07:00:00Z"));

    LearningEvent e2 = mock(LearningEvent.class);
    when(e2.getId()).thenReturn(UUID.randomUUID());
    when(e2.getEventType()).thenReturn("COMPLETED");
    when(e2.getLocalDate()).thenReturn(LocalDate.of(2026, 10, 25));
    when(e2.getOccurredAt()).thenReturn(Instant.parse("2026-10-25T08:00:00Z"));

    when(learningEventRepository.findByEnrollmentIdOrderByOccurredAtDesc(enrollmentId))
        .thenReturn(List.of(e2, e1));

    EnrollmentAnalyticsResponse response = analyticsService.getAnalytics(enrollmentId);

    // Current streak is 0, but longest streak is 2
    assertThat(response.streak().currentStreakDays()).isEqualTo(0);
    assertThat(response.streak().longestStreakDays()).isEqualTo(2);
    assertThat(response.streak().totalActiveDays()).isEqualTo(2);
    assertThat(response.streak().lastActiveDate()).isEqualTo(LocalDate.of(2026, 10, 25));
  }
}
