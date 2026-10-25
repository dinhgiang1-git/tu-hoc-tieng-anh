package vn.englishlearning.enrollment.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import vn.englishlearning.enrollment.api.EnrollmentResponses;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.ProgressPolicy;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

@Service
@Transactional(readOnly = true)
public class EnrollmentQueryService {

  private final EnrollmentRepository enrollmentRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final CurriculumRevisionRepository curriculumRevisionRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ActivityProgressRepository activityProgressRepository;
  private final CurrentUser currentUser;

  public EnrollmentQueryService(
      EnrollmentRepository enrollmentRepository,
      LessonScheduleRepository lessonScheduleRepository,
      CurriculumRevisionRepository curriculumRevisionRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ActivityProgressRepository activityProgressRepository,
      CurrentUser currentUser) {
    this.enrollmentRepository = enrollmentRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.curriculumRevisionRepository = curriculumRevisionRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.activityProgressRepository = activityProgressRepository;
    this.currentUser = currentUser;
  }

  public EnrollmentResponses.EnrollmentDetailResponse getDetail(UUID enrollmentId) {
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

    return new EnrollmentResponses.EnrollmentDetailResponse(
        enrollment.getId(),
        enrollment.getUserId(),
        enrollment.getCurriculumId(),
        enrollment.getRevisionId(),
        enrollment.getStatus(),
        enrollment.getStartDate(),
        enrollment.getTimeZone(),
        enrollment.getStudyDaysList(),
        enrollment.getDailyTargetMinutes(),
        enrollment.getVersion());
  }

  public EnrollmentResponses.LessonSummaryResponse getLessonSummary(
      UUID enrollmentId, UUID lessonId) {
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

    LessonSummaryDto lesson =
        lessonRepository
            .findLessonSummaryById(lessonId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found: " + lessonId));

    if (!enrollment.getRevisionId().equals(lesson.getRevisionId())) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Lesson does not belong to enrollment revision: " + lessonId);
    }

    RevisionInfoDto revision =
        curriculumRevisionRepository.findRevisionInfoById(enrollment.getRevisionId()).orElse(null);
    String curriculumTitle = revision != null ? revision.getTitle() : "Lộ trình";

    LessonSchedule schedule =
        lessonScheduleRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId).orElse(null);
    var plannedDate = schedule != null ? schedule.getPlannedDate() : null;

    return new EnrollmentResponses.LessonSummaryResponse(
        enrollment.getId(),
        lesson.getId(),
        curriculumTitle,
        lesson.getTitle(),
        lesson.getEstimatedMinutes(),
        plannedDate,
        lesson.isRequired());
  }

  public List<EnrollmentResponses.EnrollmentSummaryResponse> getMyEnrollments() {
    UUID currentUserId = currentUser.requireUserId();
    List<Enrollment> enrollments =
        enrollmentRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
    if (enrollments.isEmpty()) {
      return Collections.emptyList();
    }

    List<UUID> enrollmentIds = enrollments.stream().map(Enrollment::getId).toList();
    List<UUID> revisionIds =
        enrollments.stream().map(Enrollment::getRevisionId).distinct().toList();

    // 1. Batch load revision info without selecting heavy description/authors
    Map<UUID, RevisionInfoDto> revisionMap =
        curriculumRevisionRepository.findRevisionInfosByIds(revisionIds).stream()
            .collect(Collectors.toMap(RevisionInfoDto::getId, r -> r, (r1, r2) -> r1));

    // 2. Batch load lesson summaries without content_markdown
    List<LessonSummaryDto> lessonSummaries =
        lessonRepository.findLessonSummariesByRevisionIds(revisionIds);
    Map<UUID, List<LessonSummaryDto>> lessonsByRevision =
        lessonSummaries.stream().collect(Collectors.groupingBy(LessonSummaryDto::getRevisionId));

    // 3. Batch load activity summaries without instructions or skill_tags
    List<ActivitySummaryDto> activitySummaries =
        activityRepository.findActivitySummariesByRevisionIds(revisionIds);
    Map<UUID, List<ActivitySummaryDto>> activitiesByLesson =
        activitySummaries.stream().collect(Collectors.groupingBy(ActivitySummaryDto::getLessonId));

    // 4. Batch load activity progress without user_response
    List<ActivityProgressDto> progressDtos =
        activityProgressRepository.findProgressDtosByEnrollmentIds(enrollmentIds);
    Map<UUID, Map<UUID, ActivityProgressDto>> progressByEnrollment =
        progressDtos.stream()
            .collect(
                Collectors.groupingBy(
                    ActivityProgressDto::getEnrollmentId,
                    Collectors.toMap(ActivityProgressDto::getActivityId, p -> p, (p1, p2) -> p1)));

    List<EnrollmentResponses.EnrollmentSummaryResponse> results = new ArrayList<>();
    for (Enrollment enrollment : enrollments) {
      UUID revId = enrollment.getRevisionId();
      RevisionInfoDto revInfo = revisionMap.get(revId);
      String curriculumTitle = revInfo != null ? revInfo.getTitle() : "Curriculum";
      String revisionVersion = revInfo != null ? revInfo.getVersion() : "v1.0.0";

      List<LessonSummaryDto> requiredLessons =
          lessonsByRevision.getOrDefault(revId, Collections.emptyList());

      List<UUID> effectivelyRequiredActivityIds = new ArrayList<>();
      for (LessonSummaryDto lesson : requiredLessons) {
        List<ActivitySummaryDto> activities =
            activitiesByLesson.getOrDefault(lesson.getId(), Collections.emptyList());
        for (ActivitySummaryDto activity : activities) {
          if (ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), activity.isRequired())) {
            effectivelyRequiredActivityIds.add(activity.getId());
          }
        }
      }

      int totalRequired = effectivelyRequiredActivityIds.size();
      Map<UUID, ActivityProgressDto> progressMap =
          progressByEnrollment.getOrDefault(enrollment.getId(), Collections.emptyMap());

      int completedRequired = 0;
      for (UUID actId : effectivelyRequiredActivityIds) {
        ActivityProgressDto progress = progressMap.get(actId);
        if (progress != null && progress.getStatus() == ActivityStatus.COMPLETED) {
          completedRequired++;
        }
      }

      BigDecimal progressPercent = ProgressPolicy.percentOrNull(completedRequired, totalRequired);

      results.add(
          new EnrollmentResponses.EnrollmentSummaryResponse(
              enrollment.getId(),
              curriculumTitle,
              revisionVersion,
              enrollment.getStatus(),
              completedRequired,
              totalRequired,
              progressPercent,
              enrollment.getVersion()));
    }

    return results;
  }
}
