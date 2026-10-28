package vn.englishlearning.learning.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.assessment.domain.QuizAttempt;
import vn.englishlearning.assessment.infrastructure.QuizAttemptRepository;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.infrastructure.ActivityProjectionDto;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.LessonSummaryDto;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.learning.api.AnalyticsResponses.DailyActivityHistoryDto;
import vn.englishlearning.learning.api.AnalyticsResponses.DailyEventDto;
import vn.englishlearning.learning.api.AnalyticsResponses.EnrollmentAnalyticsResponse;
import vn.englishlearning.learning.api.AnalyticsResponses.QuizSummaryDto;
import vn.englishlearning.learning.api.AnalyticsResponses.StreakStatsDto;
import vn.englishlearning.learning.api.AnalyticsResponses.WeeklyProgressItemDto;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.LearningEvent;
import vn.englishlearning.learning.domain.ProgressPolicy;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.learning.infrastructure.LearningEventRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

@Service
@Transactional(readOnly = true)
public class LearningAnalyticsService {

  private static final Set<String> ACTIVE_EVENT_TYPES =
      Set.of("COMPLETED", "SUBMITTED", "QUIZ_SUBMITTED");

  private final CurrentUser currentUser;
  private final EnrollmentRepository enrollmentRepository;
  private final CurriculumRevisionRepository curriculumRevisionRepository;
  private final CurriculumWeekRepository curriculumWeekRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ActivityProgressRepository activityProgressRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final LearningEventRepository learningEventRepository;
  private final QuizAttemptRepository quizAttemptRepository;
  private final Clock clock;

  public LearningAnalyticsService(
      CurrentUser currentUser,
      EnrollmentRepository enrollmentRepository,
      CurriculumRevisionRepository curriculumRevisionRepository,
      CurriculumWeekRepository curriculumWeekRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ActivityProgressRepository activityProgressRepository,
      LessonScheduleRepository lessonScheduleRepository,
      LearningEventRepository learningEventRepository,
      QuizAttemptRepository quizAttemptRepository,
      Clock clock) {
    this.currentUser = currentUser;
    this.enrollmentRepository = enrollmentRepository;
    this.curriculumRevisionRepository = curriculumRevisionRepository;
    this.curriculumWeekRepository = curriculumWeekRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.activityProgressRepository = activityProgressRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.learningEventRepository = learningEventRepository;
    this.quizAttemptRepository = quizAttemptRepository;
    this.clock = clock;
  }

  public EnrollmentAnalyticsResponse getAnalytics(UUID enrollmentId) {
    Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
    UUID currentUserId = currentUser.requireUserId();

    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId));

    if (!enrollment.getUserId().equals(currentUserId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId);
    }

    UUID revisionId = enrollment.getRevisionId();
    String curriculumTitle =
        curriculumRevisionRepository
            .findById(revisionId)
            .map(CurriculumRevision::getTitle)
            .orElse("Lộ trình học tập");

    ZoneId enrollmentZone;
    try {
      enrollmentZone = ZoneId.of(enrollment.getTimeZone());
    } catch (Exception e) {
      enrollmentZone = ZoneId.systemDefault();
    }
    LocalDate today = LocalDate.now(clock.withZone(enrollmentZone));

    List<CurriculumWeek> weeks = curriculumWeekRepository.findWeeksByRevisionIdOrdered(revisionId);

    List<LessonSummaryDto> lessons = lessonRepository.findLessonSummariesByRevisionId(revisionId);
    List<ActivityProjectionDto> activities =
        activityRepository.findActivityProjectionsByRevisionId(revisionId);

    Map<UUID, LessonSummaryDto> lessonMap =
        lessons.stream().collect(Collectors.toMap(LessonSummaryDto::getId, l -> l));
    Map<UUID, ActivityProjectionDto> activityMap =
        activities.stream().collect(Collectors.toMap(ActivityProjectionDto::id, a -> a));

    Map<UUID, List<LessonSummaryDto>> lessonsByWeek =
        lessons.stream().collect(Collectors.groupingBy(LessonSummaryDto::getWeekId));
    Map<UUID, List<ActivityProjectionDto>> activitiesByLesson =
        activities.stream().collect(Collectors.groupingBy(ActivityProjectionDto::lessonId));

    List<ActivityProgress> progressList =
        activityProgressRepository.findByEnrollmentId(enrollmentId);
    Map<UUID, ActivityProgress> progressMap =
        progressList.stream()
            .collect(Collectors.toMap(ActivityProgress::getActivityId, p -> p, (p1, p2) -> p1));

    List<LessonSchedule> schedules =
        lessonScheduleRepository.findByEnrollmentIdOrderByPlannedDateAsc(enrollmentId);
    Map<UUID, LessonSchedule> scheduleMap =
        schedules.stream()
            .collect(Collectors.toMap(LessonSchedule::getLessonId, s -> s, (s1, s2) -> s1));

    // Calculate total required activities and completion across the entire revision
    int totalRequiredActivities = 0;
    int completedRequiredActivities = 0;
    for (LessonSummaryDto lesson : lessons) {
      List<ActivityProjectionDto> acts =
          activitiesByLesson.getOrDefault(lesson.getId(), Collections.emptyList());
      for (ActivityProjectionDto act : acts) {
        if (ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), act.isRequired())) {
          totalRequiredActivities++;
          ActivityProgress p = progressMap.get(act.id());
          if (p != null && p.getStatus() == ActivityStatus.COMPLETED) {
            completedRequiredActivities++;
          }
        }
      }
    }
    BigDecimal progressPercent =
        ProgressPolicy.percentOrNull(completedRequiredActivities, totalRequiredActivities);

    // Calculate lesson completion & estimated minutes
    int completedLessons = 0;
    int totalLessons = lessons.size();
    int estimatedCompletedMinutes = 0;
    int estimatedTotalMinutes =
        lessons.stream().mapToInt(LessonSummaryDto::getEstimatedMinutes).sum();

    for (LessonSummaryDto lesson : lessons) {
      boolean isLessonDone =
          scheduleMap.containsKey(lesson.getId())
              && scheduleMap.get(lesson.getId()).getCompletedAt() != null;
      if (!isLessonDone) {
        List<ActivityProjectionDto> acts =
            activitiesByLesson.getOrDefault(lesson.getId(), Collections.emptyList());
        int lReq = 0;
        int lDone = 0;
        for (ActivityProjectionDto a : acts) {
          if (ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), a.isRequired())) {
            lReq++;
            ActivityProgress p = progressMap.get(a.id());
            if (p != null && p.getStatus() == ActivityStatus.COMPLETED) {
              lDone++;
            }
          }
        }
        if (lReq > 0 && lDone == lReq) {
          isLessonDone = true;
        }
      }

      if (isLessonDone) {
        completedLessons++;
        estimatedCompletedMinutes += lesson.getEstimatedMinutes();
      }
    }

    // Weekly progress calculation
    List<WeeklyProgressItemDto> weeklyProgressList = new ArrayList<>();
    for (CurriculumWeek week : weeks) {
      List<LessonSummaryDto> weekLessons =
          lessonsByWeek.getOrDefault(week.getId(), Collections.emptyList());
      int weekTotalReq = 0;
      int weekCompletedReq = 0;
      int weekCompletedLessons = 0;
      int weekTotalLessons = weekLessons.size();
      int weekEstimatedMinutes = 0;

      for (LessonSummaryDto l : weekLessons) {
        weekEstimatedMinutes += l.getEstimatedMinutes();
        boolean isLessonDone =
            scheduleMap.containsKey(l.getId())
                && scheduleMap.get(l.getId()).getCompletedAt() != null;
        List<ActivityProjectionDto> acts =
            activitiesByLesson.getOrDefault(l.getId(), Collections.emptyList());
        int lReq = 0;
        int lDone = 0;
        for (ActivityProjectionDto a : acts) {
          if (ProgressPolicy.isEffectivelyRequired(l.isRequired(), a.isRequired())) {
            weekTotalReq++;
            lReq++;
            ActivityProgress p = progressMap.get(a.id());
            if (p != null && p.getStatus() == ActivityStatus.COMPLETED) {
              weekCompletedReq++;
              lDone++;
            }
          }
        }
        if (!isLessonDone && lReq > 0 && lDone == lReq) {
          isLessonDone = true;
        }
        if (isLessonDone) {
          weekCompletedLessons++;
        }
      }

      BigDecimal weekPercent = ProgressPolicy.percentOrNull(weekCompletedReq, weekTotalReq);
      String weekStatus = "NOT_STARTED";
      if (weekTotalReq > 0 && weekCompletedReq == weekTotalReq) {
        weekStatus = "COMPLETED";
      } else if (weekCompletedReq > 0) {
        weekStatus = "IN_PROGRESS";
      }

      weeklyProgressList.add(
          new WeeklyProgressItemDto(
              week.getId(),
              week.getWeekNumber(),
              week.getTitle(),
              weekTotalReq,
              weekCompletedReq,
              weekPercent,
              weekTotalLessons,
              weekCompletedLessons,
              weekEstimatedMinutes,
              weekStatus));
    }

    // Daily events & streak
    List<LearningEvent> events =
        learningEventRepository.findByEnrollmentIdOrderByOccurredAtDesc(enrollmentId);

    Set<LocalDate> activeDates = new HashSet<>();
    Map<LocalDate, List<LearningEvent>> eventsByDate = new LinkedHashMap<>();

    for (LearningEvent event : events) {
      LocalDate eventDate =
          event.getLocalDate() != null
              ? event.getLocalDate()
              : event.getOccurredAt().atZone(enrollmentZone).toLocalDate();

      if (ACTIVE_EVENT_TYPES.contains(event.getEventType())) {
        activeDates.add(eventDate);
        eventsByDate.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
      }
    }

    for (ActivityProgress p : progressList) {
      if (p.getCompletedAt() != null) {
        activeDates.add(p.getCompletedAt().atZone(enrollmentZone).toLocalDate());
      }
    }

    List<DailyActivityHistoryDto> dailyHistoryList = new ArrayList<>();
    eventsByDate.forEach(
        (date, dateEvents) -> {
          int completedCount =
              (int) dateEvents.stream().filter(e -> "COMPLETED".equals(e.getEventType())).count();
          int quizCount =
              (int)
                  dateEvents.stream()
                      .filter(e -> "QUIZ_SUBMITTED".equals(e.getEventType()))
                      .count();

          List<DailyEventDto> dailyEvents =
              dateEvents.stream()
                  .map(
                      e -> {
                        ActivityProjectionDto act =
                            e.getActivityId() != null ? activityMap.get(e.getActivityId()) : null;
                        String actTitle = act != null ? act.title() : "Hoạt động học tập";
                        String lesTitle =
                            act != null && lessonMap.containsKey(act.lessonId())
                                ? lessonMap.get(act.lessonId()).getTitle()
                                : "";
                        return new DailyEventDto(
                            e.getId(), e.getEventType(), e.getOccurredAt(), actTitle, lesTitle);
                      })
                  .toList();

          dailyHistoryList.add(
              new DailyActivityHistoryDto(date, completedCount, quizCount, dailyEvents));
        });

    // Streak stats
    int totalActiveDays = activeDates.size();
    LocalDate lastActiveDate = activeDates.stream().max(Comparator.naturalOrder()).orElse(null);

    int currentStreak = 0;
    LocalDate checkDate = today;
    if (!activeDates.contains(today)) {
      checkDate = today.minusDays(1);
    }
    while (activeDates.contains(checkDate)) {
      currentStreak++;
      checkDate = checkDate.minusDays(1);
    }

    List<LocalDate> sortedDates = activeDates.stream().sorted().toList();
    int longestStreak = 0;
    int currentRun = 0;
    LocalDate prev = null;
    for (LocalDate d : sortedDates) {
      if (prev == null || d.equals(prev.plusDays(1))) {
        currentRun++;
      } else if (!d.equals(prev)) {
        currentRun = 1;
      }
      if (currentRun > longestStreak) {
        longestStreak = currentRun;
      }
      prev = d;
    }

    StreakStatsDto streak =
        new StreakStatsDto(currentStreak, longestStreak, totalActiveDays, lastActiveDate);

    // Quiz summary
    List<QuizAttempt> attempts =
        quizAttemptRepository.findByEnrollmentIdWithAnswersOrderBySubmittedAtDesc(enrollmentId);
    QuizSummaryDto quizSummary = null;
    if (!attempts.isEmpty()) {
      int totalQuizzes = attempts.size();
      int passedQuizzes = (int) attempts.stream().filter(QuizAttempt::isPassed).count();
      double avg =
          attempts.stream().mapToDouble(QuizAttempt::getScorePercentage).average().orElse(0.0);

      BigDecimal avgPercent = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
      quizSummary = new QuizSummaryDto(totalQuizzes, passedQuizzes, avgPercent);
    }

    return new EnrollmentAnalyticsResponse(
        enrollment.getId(),
        curriculumTitle,
        enrollment.getStatus().name(),
        enrollment.getStartDate(),
        enrollment.getTimeZone(),
        enrollment.getVersion(),
        completedRequiredActivities,
        totalRequiredActivities,
        progressPercent,
        completedLessons,
        totalLessons,
        estimatedCompletedMinutes,
        estimatedTotalMinutes,
        streak,
        quizSummary,
        weeklyProgressList,
        dailyHistoryList);
  }
}
