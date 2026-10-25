package vn.englishlearning.learning.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.learning.api.TodayResponses;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.ProgressPolicy;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

@Service
@Transactional(readOnly = true)
public class TodayQueryService {

  private final Clock clock;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ActivityProgressRepository activityProgressRepository;
  private final CurriculumRevisionRepository curriculumRevisionRepository;
  private final LearnerPreferencesRepository learnerPreferencesRepository;
  private final CurrentUser currentUser;

  public TodayQueryService(
      Clock clock,
      EnrollmentRepository enrollmentRepository,
      LessonScheduleRepository lessonScheduleRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ActivityProgressRepository activityProgressRepository,
      CurriculumRevisionRepository curriculumRevisionRepository,
      LearnerPreferencesRepository learnerPreferencesRepository,
      CurrentUser currentUser) {
    this.clock = clock;
    this.enrollmentRepository = enrollmentRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.activityProgressRepository = activityProgressRepository;
    this.curriculumRevisionRepository = curriculumRevisionRepository;
    this.learnerPreferencesRepository = learnerPreferencesRepository;
    this.currentUser = currentUser;
  }

  public TodayResponses.TodayResponse getToday(UUID enrollmentIdParam) {
    UUID currentUserId = currentUser.requireUserId();

    Enrollment enrollment = resolveEnrollment(currentUserId, enrollmentIdParam);
    if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      return buildNoEnrollmentResponse(currentUserId);
    }

    ZoneId zoneId = ZoneId.of(enrollment.getTimeZone());
    LocalDate todayDate = LocalDate.now(clock.withZone(zoneId));
    boolean isRestDay = !enrollment.getStudyDaysSet().contains(todayDate.getDayOfWeek());

    UUID revisionId = enrollment.getRevisionId();
    RevisionInfoDto revision =
        curriculumRevisionRepository.findRevisionInfoById(revisionId).orElse(null);
    String curriculumTitle = revision != null ? revision.getTitle() : null;
    Long enrollmentVersion = enrollment.getVersion();

    // Load pedagogical ordered required lessons without content_markdown
    List<LessonSummaryDto> requiredLessons =
        lessonRepository.findLessonSummariesByRevisionId(revisionId);

    // Batch load activities for the revision without heavy instructions or skill_tags
    List<ActivitySummaryDto> revisionActivities =
        activityRepository.findActivitySummariesByRevisionId(revisionId);
    Map<UUID, List<ActivitySummaryDto>> activitiesByLessonId =
        revisionActivities.stream().collect(Collectors.groupingBy(ActivitySummaryDto::getLessonId));

    List<UUID> effectivelyRequiredActivityIds = new ArrayList<>();
    for (LessonSummaryDto lesson : requiredLessons) {
      List<ActivitySummaryDto> acts =
          activitiesByLessonId.getOrDefault(lesson.getId(), Collections.emptyList());
      for (ActivitySummaryDto act : acts) {
        if (ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), act.isRequired())) {
          effectivelyRequiredActivityIds.add(act.getId());
        }
      }
    }

    int totalRequired = effectivelyRequiredActivityIds.size();

    // Batch load progress without heavy user_response
    List<ActivityProgressDto> progressDtos =
        activityProgressRepository.findProgressDtosByEnrollmentId(enrollment.getId());
    Map<UUID, ActivityProgressDto> progressMap =
        progressDtos.stream()
            .collect(Collectors.toMap(ActivityProgressDto::getActivityId, p -> p, (p1, p2) -> p1));

    int completedRequired = 0;
    for (UUID actId : effectivelyRequiredActivityIds) {
      ActivityProgressDto progress = progressMap.get(actId);
      if (progress != null && progress.getStatus() == ActivityStatus.COMPLETED) {
        completedRequired++;
      }
    }

    BigDecimal progressPercent = ProgressPolicy.percentOrNull(completedRequired, totalRequired);
    TodayResponses.ProgressSummaryResponse progressSummary =
        new TodayResponses.ProgressSummaryResponse(
            completedRequired, totalRequired, progressPercent, enrollmentVersion);

    // 1. Check ABANDONED -> NO_ENROLLMENT
    if (enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      return buildNoEnrollmentResponse(currentUserId);
    }

    // 2. Check PAUSED (guard PAUSED before completed count!)
    if (enrollment.getStatus() == EnrollmentStatus.PAUSED) {
      return new TodayResponses.TodayResponse(
          "PAUSED",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          isRestDay,
          enrollmentVersion,
          null,
          Collections.emptyList(),
          progressSummary);
    }

    // 3. Check CURRICULUM_COMPLETED
    if (enrollment.getStatus() == EnrollmentStatus.COMPLETED
        || (totalRequired > 0 && completedRequired == totalRequired)) {
      return new TodayResponses.TodayResponse(
          "CURRICULUM_COMPLETED",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          isRestDay,
          enrollmentVersion,
          new TodayResponses.PrimaryActionDto(
              "CURRICULUM_COMPLETED",
              "Chúc mừng bạn đã hoàn thành 100% nhiệm vụ bắt buộc của lộ trình!",
              null,
              null,
              null,
              null,
              null),
          Collections.emptyList(),
          progressSummary);
    }

    // 4. Process ACTIVE enrollment
    Map<UUID, LessonSchedule> scheduleMap =
        lessonScheduleRepository
            .findByEnrollmentIdOrderByPlannedDateAsc(enrollment.getId())
            .stream()
            .collect(Collectors.toMap(LessonSchedule::getLessonId, s -> s, (s1, s2) -> s1));

    List<LessonStatusInfo> uncompletedLessons = new ArrayList<>();
    for (LessonSummaryDto lesson : requiredLessons) {
      List<ActivitySummaryDto> acts =
          activitiesByLessonId.getOrDefault(lesson.getId(), Collections.emptyList());
      int totalAct = acts.size();
      int completedAct = 0;
      int requiredActCount = 0;
      int completedRequiredActCount = 0;
      boolean hasInProgressAct = false;
      for (ActivitySummaryDto a : acts) {
        boolean isEffRequired =
            ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), a.isRequired());
        if (isEffRequired) {
          requiredActCount++;
        }
        ActivityProgressDto ap = progressMap.get(a.getId());
        if (ap != null) {
          if (ap.getStatus() == ActivityStatus.COMPLETED) {
            completedAct++;
            if (isEffRequired) {
              completedRequiredActCount++;
            }
          } else if (ap.getStatus() == ActivityStatus.IN_PROGRESS) {
            hasInProgressAct = true;
          }
        }
      }

      // Lesson completion is strictly based on its effectively required activities!
      boolean isCompleted =
          requiredActCount > 0 ? (completedRequiredActCount == requiredActCount) : true;
      if (!isCompleted) {
        boolean isInProgress = completedAct > 0 || hasInProgressAct;
        LessonSchedule sched = scheduleMap.get(lesson.getId());
        LocalDate plannedDate = sched != null ? sched.getPlannedDate() : null;
        uncompletedLessons.add(
            new LessonStatusInfo(lesson, plannedDate, completedAct, totalAct, isInProgress));
      }
    }

    // Priority 1: IN_PROGRESS
    LessonStatusInfo inProgressLesson =
        uncompletedLessons.stream()
            .filter(l -> l.isInProgress)
            .min(
                Comparator.comparing(
                        (LessonStatusInfo l) ->
                            l.plannedDate != null ? l.plannedDate : LocalDate.MAX)
                    .thenComparingInt(l -> l.lesson.getOrderIndex()))
            .orElse(null);

    if (inProgressLesson != null) {
      List<TodayResponses.OverdueLessonDto> overdueLessons =
          uncompletedLessons.stream()
              .filter(
                  l ->
                      l.plannedDate != null
                          && l.plannedDate.isBefore(todayDate)
                          && !Objects.equals(l.lesson.getId(), inProgressLesson.lesson.getId()))
              .sorted(
                  Comparator.comparing((LessonStatusInfo l) -> l.plannedDate)
                      .thenComparingInt(l -> l.lesson.getOrderIndex()))
              .map(
                  l ->
                      new TodayResponses.OverdueLessonDto(
                          l.lesson.getId(),
                          l.lesson.getTitle(),
                          l.plannedDate,
                          (int) ChronoUnit.DAYS.between(l.plannedDate, todayDate)))
              .toList();

      return new TodayResponses.TodayResponse(
          "IN_PROGRESS",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          isRestDay,
          enrollmentVersion,
          new TodayResponses.PrimaryActionDto(
              "IN_PROGRESS",
              "Bạn đang học dở "
                  + inProgressLesson.lesson.getTitle()
                  + ". Hãy tiếp tục để hoàn thành mục tiêu!",
              inProgressLesson.lesson.getId(),
              inProgressLesson.lesson.getTitle(),
              inProgressLesson.lesson.getEstimatedMinutes(),
              inProgressLesson.completedAct,
              inProgressLesson.totalAct),
          overdueLessons,
          progressSummary);
    }

    // Priority 2: OVERDUE
    List<LessonStatusInfo> overdueList =
        uncompletedLessons.stream()
            .filter(l -> l.plannedDate != null && l.plannedDate.isBefore(todayDate))
            .sorted(
                Comparator.comparing((LessonStatusInfo l) -> l.plannedDate)
                    .thenComparingInt(l -> l.lesson.getOrderIndex()))
            .toList();

    if (!overdueList.isEmpty()) {
      LessonStatusInfo earliestOverdue = overdueList.get(0);
      List<TodayResponses.OverdueLessonDto> secondaryOverdue =
          overdueList.subList(1, overdueList.size()).stream()
              .map(
                  l ->
                      new TodayResponses.OverdueLessonDto(
                          l.lesson.getId(),
                          l.lesson.getTitle(),
                          l.plannedDate,
                          (int) ChronoUnit.DAYS.between(l.plannedDate, todayDate)))
              .toList();

      return new TodayResponses.TodayResponse(
          "OVERDUE",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          isRestDay,
          enrollmentVersion,
          new TodayResponses.PrimaryActionDto(
              "OVERDUE",
              "Buổi học đã quá hạn: "
                  + earliestOverdue.lesson.getTitle()
                  + ". Hãy học bù để bắt kịp tiến độ!",
              earliestOverdue.lesson.getId(),
              earliestOverdue.lesson.getTitle(),
              earliestOverdue.lesson.getEstimatedMinutes(),
              earliestOverdue.completedAct,
              earliestOverdue.totalAct),
          secondaryOverdue,
          progressSummary);
    }

    // Priority 3: DUE_TODAY
    LessonStatusInfo dueTodayLesson =
        uncompletedLessons.stream()
            .filter(l -> l.plannedDate != null && l.plannedDate.isEqual(todayDate))
            .min(Comparator.comparingInt(l -> l.lesson.getOrderIndex()))
            .orElse(null);

    if (dueTodayLesson != null) {
      return new TodayResponses.TodayResponse(
          "DUE_TODAY",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          isRestDay,
          enrollmentVersion,
          new TodayResponses.PrimaryActionDto(
              "DUE_TODAY",
              "Buổi học đến hạn hôm nay: " + dueTodayLesson.lesson.getTitle(),
              dueTodayLesson.lesson.getId(),
              dueTodayLesson.lesson.getTitle(),
              dueTodayLesson.lesson.getEstimatedMinutes(),
              dueTodayLesson.completedAct,
              dueTodayLesson.totalAct),
          Collections.emptyList(),
          progressSummary);
    }

    // Priority 4: REST_DAY / UPCOMING
    LessonStatusInfo nextLesson =
        uncompletedLessons.stream()
            .filter(l -> l.plannedDate != null && l.plannedDate.isAfter(todayDate))
            .min(
                Comparator.comparing((LessonStatusInfo l) -> l.plannedDate)
                    .thenComparingInt(l -> l.lesson.getOrderIndex()))
            .orElse(null);

    if (isRestDay) {
      TodayResponses.PrimaryActionDto primaryAction =
          nextLesson != null
              ? new TodayResponses.PrimaryActionDto(
                  "UPCOMING",
                  "Hôm nay là ngày nghỉ theo lịch của bạn. Bạn có thể học trước bài tiếp theo nếu muốn!",
                  nextLesson.lesson.getId(),
                  nextLesson.lesson.getTitle(),
                  nextLesson.lesson.getEstimatedMinutes(),
                  nextLesson.completedAct,
                  nextLesson.totalAct)
              : null;

      return new TodayResponses.TodayResponse(
          "REST_DAY",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          true,
          enrollmentVersion,
          primaryAction,
          Collections.emptyList(),
          progressSummary);
    } else {
      TodayResponses.PrimaryActionDto primaryAction =
          nextLesson != null
              ? new TodayResponses.PrimaryActionDto(
                  "UPCOMING",
                  "Bạn đã hoàn thành bài học hôm nay! Bạn có thể học trước bài tiếp theo.",
                  nextLesson.lesson.getId(),
                  nextLesson.lesson.getTitle(),
                  nextLesson.lesson.getEstimatedMinutes(),
                  nextLesson.completedAct,
                  nextLesson.totalAct)
              : null;

      return new TodayResponses.TodayResponse(
          "UPCOMING",
          enrollment.getId(),
          revisionId,
          curriculumTitle,
          todayDate,
          false,
          enrollmentVersion,
          primaryAction,
          Collections.emptyList(),
          progressSummary);
    }
  }

  private Enrollment resolveEnrollment(UUID currentUserId, UUID enrollmentIdParam) {
    if (enrollmentIdParam != null) {
      Enrollment enrollment =
          enrollmentRepository
              .findById(enrollmentIdParam)
              .orElseThrow(
                  () ->
                      new DomainException(
                          ErrorCode.RESOURCE_NOT_FOUND,
                          "Enrollment not found: " + enrollmentIdParam));
      if (!enrollment.getUserId().equals(currentUserId)) {
        throw new DomainException(
            ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentIdParam);
      }
      return enrollment;
    }

    LearnerPreferences preferences =
        learnerPreferencesRepository.findById(currentUserId).orElse(null);
    if (preferences == null || preferences.getFocusedEnrollmentId() == null) {
      return null;
    }

    return enrollmentRepository
        .findById(preferences.getFocusedEnrollmentId())
        .filter(e -> e.getUserId().equals(currentUserId))
        .orElse(null);
  }

  private TodayResponses.TodayResponse buildNoEnrollmentResponse(UUID currentUserId) {
    String timezone =
        learnerPreferencesRepository
            .findById(currentUserId)
            .map(LearnerPreferences::getTimezone)
            .orElse("Asia/Ho_Chi_Minh");
    ZoneId zoneId = ZoneId.of(timezone);
    LocalDate todayDate = LocalDate.now(clock.withZone(zoneId));

    return new TodayResponses.TodayResponse(
        "NO_ENROLLMENT",
        null,
        null,
        null,
        todayDate,
        false,
        null,
        null,
        Collections.emptyList(),
        null);
  }

  private static class LessonStatusInfo {
    final LessonSummaryDto lesson;
    final LocalDate plannedDate;
    final int completedAct;
    final int totalAct;
    final boolean isInProgress;

    LessonStatusInfo(
        LessonSummaryDto lesson,
        LocalDate plannedDate,
        int completedAct,
        int totalAct,
        boolean isInProgress) {
      this.lesson = lesson;
      this.plannedDate = plannedDate;
      this.completedAct = completedAct;
      this.totalAct = totalAct;
      this.isInProgress = isInProgress;
    }
  }
}
