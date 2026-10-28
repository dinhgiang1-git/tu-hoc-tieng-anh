package vn.englishlearning.learning.application;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.domain.Resource;
import vn.englishlearning.curriculum.infrastructure.ActivityProjectionDto;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.ActivitySummaryDto;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.LessonSummaryDto;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.learning.api.LearningProgressRequests.SaveResponseDraftRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.SubmitResponseRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.UpdateProgressRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.ActivityResponseDto;
import vn.englishlearning.learning.api.LearningProgressResponses.ActivityWorkspaceDto;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonActivitiesResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonActivityItemDto;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonDetailResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressSummaryResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ResourceDto;
import vn.englishlearning.learning.domain.ActivityProgress;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.learning.domain.LearningEvent;
import vn.englishlearning.learning.domain.ProgressPolicy;
import vn.englishlearning.learning.infrastructure.ActivityProgressDto;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;
import vn.englishlearning.learning.infrastructure.LearningEventRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;

@Service
public class LearningProgressService {

  private final Clock clock;
  private final EnrollmentRepository enrollmentRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ActivityProgressRepository activityProgressRepository;
  private final LearningEventRepository learningEventRepository;
  private final AppUserRepository userRepository;
  private final CurrentUser currentUser;
  private final EntityManager entityManager;

  public LearningProgressService(
      Clock clock,
      EnrollmentRepository enrollmentRepository,
      LessonScheduleRepository lessonScheduleRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ActivityProgressRepository activityProgressRepository,
      LearningEventRepository learningEventRepository,
      AppUserRepository userRepository,
      CurrentUser currentUser,
      EntityManager entityManager) {
    this.clock = clock;
    this.enrollmentRepository = enrollmentRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.activityProgressRepository = activityProgressRepository;
    this.learningEventRepository = learningEventRepository;
    this.userRepository = userRepository;
    this.currentUser = currentUser;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public ProgressSummaryResponse getSummary(UUID enrollmentId) {
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

    ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
    return new ProgressSummaryResponse(
        stats.completedRequired(),
        stats.totalRequired(),
        stats.progressPercent(),
        enrollment.getVersion());
  }

  @Transactional(readOnly = true)
  public LessonActivitiesResponse getLessonActivities(UUID enrollmentId, UUID lessonId) {
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

    List<ActivityProjectionDto> activityDtos =
        activityRepository.findActivityProjectionsByLessonId(lessonId);
    List<UUID> activityIds = activityDtos.stream().map(ActivityProjectionDto::id).toList();

    Map<UUID, ActivityProgress> progressMap =
        activityProgressRepository
            .findByEnrollmentIdAndActivityIdIn(enrollmentId, activityIds)
            .stream()
            .collect(Collectors.toMap(ActivityProgress::getActivityId, p -> p, (p1, p2) -> p1));

    List<LessonActivityItemDto> items =
        activityDtos.stream()
            .map(
                dto -> {
                  ActivityProgress p = progressMap.get(dto.id());
                  ActivityStatus status = p != null ? p.getStatus() : ActivityStatus.NOT_STARTED;
                  String userResponse = p != null ? p.getUserResponse() : null;
                  return new LessonActivityItemDto(
                      dto.id(),
                      dto.title(),
                      dto.type(),
                      dto.isRequired(),
                      dto.completionPolicy(),
                      status,
                      userResponse);
                })
            .toList();

    return new LessonActivitiesResponse(lessonId, enrollment.getVersion(), items);
  }

  @Transactional(readOnly = true)
  public LessonDetailResponse getLessonWorkspace(UUID enrollmentId, UUID lessonId) {
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

    Lesson lesson =
        lessonRepository
            .findById(lessonId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found: " + lessonId));

    if (!enrollment.getRevisionId().equals(lesson.getWeek().getPhase().getRevision().getId())) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Lesson does not belong to enrollment revision: " + lessonId);
    }

    List<Activity> activities = activityRepository.findActivitiesWithResourcesByLessonId(lessonId);
    List<UUID> activityIds = activities.stream().map(Activity::getId).toList();

    Map<UUID, ActivityProgress> progressMap =
        activityProgressRepository
            .findByEnrollmentIdAndActivityIdIn(enrollmentId, activityIds)
            .stream()
            .collect(Collectors.toMap(ActivityProgress::getActivityId, p -> p, (p1, p2) -> p1));

    List<ActivityWorkspaceDto> activityDtos =
        activities.stream()
            .map(
                a -> {
                  ActivityProgress p = progressMap.get(a.getId());
                  ActivityStatus status = p != null ? p.getStatus() : ActivityStatus.NOT_STARTED;
                  String userResponse = p != null ? p.getUserResponse() : null;

                  List<ResourceDto> resources =
                      a.getResources().stream()
                          .sorted(
                              Comparator.comparing(Resource::getTitle)
                                  .thenComparing(Resource::getId))
                          .map(
                              r ->
                                  new ResourceDto(
                                      r.getId(),
                                      r.getTitle(),
                                      r.getUrl(),
                                      r.getMediaType().name(),
                                      r.getAttribution()))
                          .toList();

                  return new ActivityWorkspaceDto(
                      a.getId(),
                      a.getTitle(),
                      a.getActivityType(),
                      a.isRequired(),
                      a.getCompletionPolicy(),
                      a.getInstructions(),
                      status,
                      userResponse,
                      resources);
                })
            .toList();

    // Compute nextLessonId in revision order
    List<LessonSummaryDto> summaries =
        lessonRepository.findLessonSummariesByRevisionId(enrollment.getRevisionId());
    UUID nextLessonId = null;
    for (int i = 0; i < summaries.size(); i++) {
      if (summaries.get(i).id().equals(lessonId)) {
        if (i + 1 < summaries.size()) {
          nextLessonId = summaries.get(i + 1).id();
        }
        break;
      }
    }

    return new LessonDetailResponse(
        lessonId,
        lesson.getTitle(),
        lesson.getEstimatedMinutes(),
        lesson.isRequired(),
        lesson.getContentMarkdown(),
        enrollment.getVersion(),
        nextLessonId,
        activityDtos);
  }

  @Transactional
  public ProgressResponse updateProgress(
      UUID enrollmentId, UUID activityId, UpdateProgressRequest request) {
    if (request == null || request.status() == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion phải không âm.");
    }

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

    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Activity not found: " + activityId));

    UUID activityRevisionId = activity.getLesson().getWeek().getPhase().getRevision().getId();
    if (!enrollment.getRevisionId().equals(activityRevisionId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Activity does not belong to enrollment revision: " + activityId);
    }

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED
        || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Lộ trình đang ở trạng thái " + enrollment.getStatus() + ", không thể thao tác học tập.");
    }

    if (activity.getActivityType() == ActivityType.QUIZ) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Nhiệm vụ kiểm tra (QUIZ) không hỗ trợ hoàn thành thủ công.");
    }
    if (activity.getActivityType() == ActivityType.RECORDING
        || activity.getActivityType() == ActivityType.DICTATION) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Loại nhiệm vụ chưa được hỗ trợ thao tác thủ công.");
    }

    // Concurrency check
    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, activityId)
            .orElse(null);
    ActivityStatus currentStatus =
        progress != null ? progress.getStatus() : ActivityStatus.NOT_STARTED;

    // Check No-op
    ActivityStatus requestedStatus = request.status();
    ActivityStatus targetStatus;
    if (requestedStatus == ActivityStatus.COMPLETED) {
      // Complete validations
      if (activity.getCompletionPolicy() == CompletionPolicy.MANUAL_CHECK_WITH_RESPONSE) {
        if (progress == null
            || progress.getUserResponse() == null
            || progress.getUserResponse().trim().isEmpty()) {
          throw new DomainException(
              ErrorCode.RESPONSE_REQUIRED,
              "Nhiệm vụ yêu cầu nhập câu trả lời trước khi hoàn thành.");
        }
      } else if (activity.getCompletionPolicy() == CompletionPolicy.RESPONSE_SUBMIT
          || activity.getActivityType() == ActivityType.REFLECTION) {
        if (progress == null
            || progress.getUserResponse() == null
            || progress.getUserResponse().trim().isEmpty()) {
          throw new DomainException(
              ErrorCode.RESPONSE_REQUIRED, "Nhiệm vụ phản tư yêu cầu nộp câu trả lời.");
        } else {
          throw new DomainException(
              ErrorCode.STATE_CONFLICT,
              "Nhiệm vụ phản tư phải sử dụng chức năng Nộp và Hoàn thành.");
        }
      } else if (activity.getCompletionPolicy() == CompletionPolicy.PASS_SCORE
          || activity.getCompletionPolicy() == CompletionPolicy.ON_SUBMIT) {
        throw new DomainException(
            ErrorCode.STATE_CONFLICT, "Nhiệm vụ này không cho phép hoàn thành thủ công.");
      }
      targetStatus = ActivityStatus.COMPLETED;
    } else {
      // Undo operation
      if (currentStatus == ActivityStatus.COMPLETED) {
        if (progress != null
            && progress.getUserResponse() != null
            && !progress.getUserResponse().trim().isEmpty()) {
          targetStatus = ActivityStatus.IN_PROGRESS;
        } else {
          targetStatus = ActivityStatus.NOT_STARTED;
        }
      } else {
        targetStatus = requestedStatus;
      }
    }

    if (currentStatus == targetStatus) {
      // No-op: same version, same status -> no event, no version increment
      ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
      return new ProgressResponse(
          activityId,
          currentStatus,
          enrollment.getVersion(),
          stats.completedRequired(),
          stats.totalRequired(),
          stats.progressPercent());
    }

    Instant now = Instant.now(clock);
    ZoneId zoneId = ZoneId.of(enrollment.getTimeZone());
    LocalDate localDate = LocalDate.now(clock.withZone(zoneId));

    // Reopen check if enrollment was COMPLETED and an effectively required activity is undone
    boolean isEffRequired =
        ProgressPolicy.isEffectivelyRequired(
            activity.getLesson().isRequired(), activity.isRequired());
    boolean shouldReopen = false;
    if (enrollment.getStatus() == EnrollmentStatus.COMPLETED
        && isEffRequired
        && targetStatus != ActivityStatus.COMPLETED) {
      // Lock user row consistently with CreateEnrollmentService
      userRepository.findByIdForUpdate(currentUserId);
      List<Enrollment> conflicts =
          enrollmentRepository.findByUserIdAndCurriculumIdAndStatusInAndIdNot(
              currentUserId,
              enrollment.getCurriculumId(),
              List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PAUSED),
              enrollment.getId());
      if (!conflicts.isEmpty()) {
        throw new DomainException(
            ErrorCode.STATE_CONFLICT,
            "Không thể mở lại lộ trình vì bạn đã có lộ trình đang học cho giáo trình này.");
      }
      shouldReopen = true;
    }

    if (progress == null) {
      progress = new ActivityProgress(enrollmentId, activityId, targetStatus, null, now);
    } else {
      progress.setStatus(targetStatus, now);
    }
    progress = activityProgressRepository.save(progress);

    // Record LearningEvent
    String eventType;
    if (targetStatus == ActivityStatus.COMPLETED) {
      eventType = "COMPLETED";
    } else if (currentStatus == ActivityStatus.COMPLETED) {
      eventType = "UNDONE";
    } else {
      eventType = "IN_PROGRESS";
    }
    String eventPayload = "{\"status\":\"" + targetStatus + "\"}";
    LearningEvent event =
        new LearningEvent(
            currentUserId,
            enrollmentId,
            activityId,
            eventType,
            eventPayload,
            localDate,
            enrollment.getTimeZone(),
            now);
    learningEventRepository.save(event);

    // Update lesson schedule completed_at if lesson is required
    updateLessonScheduleCompletion(enrollment, activity.getLesson().getId(), now);

    // Update enrollment completion
    EnrollmentStatus targetEnrollmentStatus = enrollment.getStatus();
    Instant targetCompletedAt = enrollment.getCompletedAt();
    if (shouldReopen) {
      targetEnrollmentStatus = EnrollmentStatus.ACTIVE;
      targetCompletedAt = null;
    }

    ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
    if (stats.totalRequired() > 0 && stats.completedRequired() == stats.totalRequired()) {
      targetEnrollmentStatus = EnrollmentStatus.COMPLETED;
      if (targetCompletedAt == null) {
        targetCompletedAt = now;
      }
    }

    Long newVersion = request.expectedVersion() + 1;
    entityManager.detach(enrollment);
    int updatedRows =
        enrollmentRepository.updateVersionAndState(
            enrollment.getId(),
            request.expectedVersion(),
            newVersion,
            targetEnrollmentStatus,
            targetCompletedAt,
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    enrollment.setStatus(targetEnrollmentStatus);
    enrollment.setCompletedAt(targetCompletedAt);
    enrollment.setVersion(newVersion);

    return new ProgressResponse(
        activityId,
        progress.getStatus(),
        newVersion,
        stats.completedRequired(),
        stats.totalRequired(),
        stats.progressPercent());
  }

  @Transactional
  public ProgressResponse submitResponse(
      UUID enrollmentId, UUID activityId, SubmitResponseRequest request) {
    if (request == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion phải không âm.");
    }
    if (request.responseText() == null || request.responseText().trim().isEmpty()) {
      throw new DomainException(
          ErrorCode.RESPONSE_REQUIRED, "Nội dung phản hồi không được để trống.");
    }
    if (request.responseText().length() > 5000) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Nội dung phản hồi không được vượt quá 5000 ký tự.");
    }

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

    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Activity not found: " + activityId));

    UUID activityRevisionId = activity.getLesson().getWeek().getPhase().getRevision().getId();
    if (!enrollment.getRevisionId().equals(activityRevisionId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Activity does not belong to enrollment revision: " + activityId);
    }

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED
        || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Lộ trình đang ở trạng thái " + enrollment.getStatus() + ", không thể thao tác học tập.");
    }

    if (activity.getActivityType() != ActivityType.PRACTICE
        && activity.getActivityType() != ActivityType.REFLECTION) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Nhiệm vụ này không hỗ trợ nộp phản hồi.");
    }
    if (activity.getCompletionPolicy() != CompletionPolicy.RESPONSE_SUBMIT
        && activity.getCompletionPolicy() != CompletionPolicy.MANUAL_CHECK_WITH_RESPONSE) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Chính sách của nhiệm vụ không hỗ trợ nộp phản hồi.");
    }

    // Concurrency check
    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    String responseText = request.responseText();
    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, activityId)
            .orElse(null);

    // No-op check: already completed with identical responseText
    if (progress != null
        && progress.getStatus() == ActivityStatus.COMPLETED
        && Objects.equals(progress.getUserResponse(), responseText)) {
      ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
      return new ProgressResponse(
          activityId,
          ActivityStatus.COMPLETED,
          enrollment.getVersion(),
          stats.completedRequired(),
          stats.totalRequired(),
          stats.progressPercent());
    }

    Instant now = Instant.now(clock);
    ZoneId zoneId = ZoneId.of(enrollment.getTimeZone());
    LocalDate localDate = LocalDate.now(clock.withZone(zoneId));

    if (progress == null) {
      progress =
          new ActivityProgress(
              enrollmentId, activityId, ActivityStatus.COMPLETED, responseText, now);
    } else {
      progress.setUserResponse(responseText, now);
      progress.setStatus(ActivityStatus.COMPLETED, now);
    }
    progress = activityProgressRepository.save(progress);

    // Record SUBMITTED event (never record response text into event payload)
    String eventPayload = "{\"status\":\"COMPLETED\"}";
    LearningEvent event =
        new LearningEvent(
            currentUserId,
            enrollmentId,
            activityId,
            "SUBMITTED",
            eventPayload,
            localDate,
            enrollment.getTimeZone(),
            now);
    learningEventRepository.save(event);

    // Update lesson schedule completed_at if lesson is required
    updateLessonScheduleCompletion(enrollment, activity.getLesson().getId(), now);

    // Update enrollment completion
    ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
    EnrollmentStatus targetEnrollmentStatus = enrollment.getStatus();
    Instant targetCompletedAt = enrollment.getCompletedAt();
    if (stats.totalRequired() > 0 && stats.completedRequired() == stats.totalRequired()) {
      targetEnrollmentStatus = EnrollmentStatus.COMPLETED;
      if (targetCompletedAt == null) {
        targetCompletedAt = now;
      }
    }

    Long newVersion = request.expectedVersion() + 1;
    entityManager.detach(enrollment);
    int updatedRows =
        enrollmentRepository.updateVersionAndState(
            enrollment.getId(),
            request.expectedVersion(),
            newVersion,
            targetEnrollmentStatus,
            targetCompletedAt,
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    enrollment.setStatus(targetEnrollmentStatus);
    enrollment.setCompletedAt(targetCompletedAt);
    enrollment.setVersion(newVersion);

    return new ProgressResponse(
        activityId,
        ActivityStatus.COMPLETED,
        newVersion,
        stats.completedRequired(),
        stats.totalRequired(),
        stats.progressPercent());
  }

  @Transactional
  public Long recordQuizAssessmentResult(
      UUID enrollmentId,
      UUID activityId,
      boolean passed,
      int score,
      int totalQuestions,
      Long expectedVersion) {
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

    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Activity not found: " + activityId));

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED
        || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Lộ trình đang ở trạng thái " + enrollment.getStatus() + ", không thể thao tác học tập.");
    }

    if (!enrollment.getVersion().equals(expectedVersion)) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    Instant now = Instant.now(clock);
    ZoneId zoneId = ZoneId.of(enrollment.getTimeZone());
    LocalDate localDate = LocalDate.now(clock.withZone(zoneId));

    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, activityId)
            .orElse(null);

    boolean wasAlreadyCompleted =
        progress != null && progress.getStatus() == ActivityStatus.COMPLETED;

    ActivityStatus targetStatus;
    if (passed || wasAlreadyCompleted) {
      targetStatus = ActivityStatus.COMPLETED;
    } else {
      targetStatus = ActivityStatus.IN_PROGRESS;
    }

    if (progress == null) {
      progress = new ActivityProgress(enrollmentId, activityId, targetStatus, null, now);
    } else {
      progress.setStatus(targetStatus, now);
    }
    activityProgressRepository.save(progress);

    String eventPayload =
        "{\"status\":\""
            + targetStatus
            + "\",\"score\":"
            + score
            + ",\"total\":"
            + totalQuestions
            + ",\"passed\":"
            + passed
            + "}";
    LearningEvent event =
        new LearningEvent(
            currentUserId,
            enrollmentId,
            activityId,
            "QUIZ_SUBMITTED",
            eventPayload,
            localDate,
            enrollment.getTimeZone(),
            now);
    learningEventRepository.save(event);

    if (targetStatus == ActivityStatus.COMPLETED) {
      updateLessonScheduleCompletion(enrollment, activity.getLesson().getId(), now);
    }

    ProgressStats stats = computeProgressStats(enrollment.getId(), enrollment.getRevisionId());
    EnrollmentStatus targetEnrollmentStatus = enrollment.getStatus();
    Instant targetCompletedAt = enrollment.getCompletedAt();
    if (stats.totalRequired() > 0 && stats.completedRequired() == stats.totalRequired()) {
      targetEnrollmentStatus = EnrollmentStatus.COMPLETED;
      if (targetCompletedAt == null) {
        targetCompletedAt = now;
      }
    }

    Long newVersion = expectedVersion + 1;
    entityManager.detach(enrollment);
    int updatedRows =
        enrollmentRepository.updateVersionAndState(
            enrollment.getId(),
            expectedVersion,
            newVersion,
            targetEnrollmentStatus,
            targetCompletedAt,
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    return newVersion;
  }

  @Transactional
  public ActivityResponseDto saveResponseDraft(
      UUID enrollmentId, UUID activityId, SaveResponseDraftRequest request) {
    if (request == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion phải không âm.");
    }

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

    if (enrollment.getStatus() == EnrollmentStatus.PAUSED
        || enrollment.getStatus() == EnrollmentStatus.ABANDONED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Lộ trình đang tạm dừng hoặc đã hủy, không thể lưu nháp.");
    }

    Activity activity =
        activityRepository
            .findById(activityId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Activity not found: " + activityId));

    if (!enrollment
        .getRevisionId()
        .equals(activity.getLesson().getWeek().getPhase().getRevision().getId())) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND,
          "Activity does not belong to enrollment revision: " + activityId);
    }

    boolean isSupported =
        (activity.getActivityType() == ActivityType.PRACTICE
                || activity.getActivityType() == ActivityType.REFLECTION)
            && (activity.getCompletionPolicy() == CompletionPolicy.MANUAL_CHECK_WITH_RESPONSE
                || activity.getCompletionPolicy() == CompletionPolicy.RESPONSE_SUBMIT);

    if (!isSupported) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT, "Nhiệm vụ này không hỗ trợ lưu nháp câu trả lời.");
    }

    ActivityProgress progress =
        activityProgressRepository
            .findByEnrollmentIdAndActivityId(enrollmentId, activityId)
            .orElse(null);

    String newText = request.responseText() != null ? request.responseText() : "";

    if (progress != null && progress.getStatus() == ActivityStatus.COMPLETED) {
      if (!Objects.equals(progress.getUserResponse(), newText)) {
        throw new DomainException(
            ErrorCode.STATE_CONFLICT,
            "Nhiệm vụ đã hoàn thành. Hãy hoàn tác trước khi lưu nháp mới.");
      }
    }

    // Check expectedVersion against enrollment version before any no-op check
    if (!Objects.equals(enrollment.getVersion(), request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    String currentText = progress != null ? progress.getUserResponse() : null;
    boolean currentBlank = (currentText == null || currentText.isEmpty());
    boolean newBlank = newText.isEmpty();

    // No-op check: both blank or identical text
    if ((progress == null && newBlank)
        || (progress != null && currentBlank && newBlank)
        || (progress != null && Objects.equals(currentText, newText))) {
      Instant updatedAt = progress != null ? progress.getUpdatedAt() : Instant.now(clock);
      UUID progressId = progress != null ? progress.getId() : UUID.randomUUID();
      return new ActivityResponseDto(
          progressId,
          activityId,
          currentText == null ? "" : currentText,
          updatedAt,
          enrollment.getVersion());
    }

    Instant now = Instant.now(clock);

    if (progress == null) {
      ActivityStatus status = newBlank ? ActivityStatus.NOT_STARTED : ActivityStatus.IN_PROGRESS;
      progress = new ActivityProgress(enrollmentId, activityId, status, newText, now);
    } else {
      if (progress.getStatus() == ActivityStatus.NOT_STARTED && !newBlank) {
        progress.setStatus(ActivityStatus.IN_PROGRESS, now);
      }
      progress.setUserResponse(newText, now);
    }
    progress = activityProgressRepository.save(progress);

    Long newVersion = request.expectedVersion() + 1;
    entityManager.detach(enrollment);
    int updatedRows =
        enrollmentRepository.updateVersionAndState(
            enrollment.getId(),
            request.expectedVersion(),
            newVersion,
            enrollment.getStatus(),
            enrollment.getCompletedAt(),
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    enrollment.setVersion(newVersion);

    return new ActivityResponseDto(
        progress.getId(),
        activityId,
        progress.getUserResponse(),
        progress.getUpdatedAt(),
        newVersion);
  }

  private void updateLessonScheduleCompletion(Enrollment enrollment, UUID lessonId, Instant now) {
    LessonSchedule schedule =
        lessonScheduleRepository
            .findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
            .orElse(null);
    if (schedule == null) {
      return;
    }

    // Check effectively required activities of this lesson
    List<ActivityProjectionDto> lessonActivities =
        activityRepository.findActivityProjectionsByLessonId(lessonId);
    LessonSummaryDto lesson = lessonRepository.findLessonSummaryById(lessonId).orElse(null);
    boolean lessonRequired = lesson != null && lesson.isRequired();

    List<UUID> effReqActivityIds =
        lessonActivities.stream()
            .filter(a -> ProgressPolicy.isEffectivelyRequired(lessonRequired, a.isRequired()))
            .map(ActivityProjectionDto::id)
            .toList();

    if (effReqActivityIds.isEmpty()) {
      return;
    }

    List<ActivityProgress> progresses =
        activityProgressRepository.findByEnrollmentIdAndActivityIdIn(
            enrollment.getId(), effReqActivityIds);
    long completedCount =
        progresses.stream().filter(p -> p.getStatus() == ActivityStatus.COMPLETED).count();

    if (completedCount == effReqActivityIds.size()) {
      if (schedule.getCompletedAt() == null) {
        schedule.setCompletedAt(now, now);
        lessonScheduleRepository.save(schedule);
      }
    } else {
      if (schedule.getCompletedAt() != null) {
        schedule.setCompletedAt(null, now);
        lessonScheduleRepository.save(schedule);
      }
    }
  }

  private record ProgressStats(
      int completedRequired, int totalRequired, BigDecimal progressPercent) {}

  private ProgressStats computeProgressStats(UUID enrollmentId, UUID revisionId) {
    List<LessonSummaryDto> requiredLessons =
        lessonRepository.findLessonSummariesByRevisionId(revisionId);
    List<ActivitySummaryDto> activitySummaries =
        activityRepository.findActivitySummariesByRevisionId(revisionId);

    Map<UUID, List<ActivitySummaryDto>> actsByLesson =
        activitySummaries.stream().collect(Collectors.groupingBy(ActivitySummaryDto::getLessonId));

    List<UUID> effectivelyRequiredActivityIds = new ArrayList<>();
    for (LessonSummaryDto lesson : requiredLessons) {
      List<ActivitySummaryDto> acts =
          actsByLesson.getOrDefault(lesson.getId(), Collections.emptyList());
      for (ActivitySummaryDto act : acts) {
        if (ProgressPolicy.isEffectivelyRequired(lesson.isRequired(), act.isRequired())) {
          effectivelyRequiredActivityIds.add(act.getId());
        }
      }
    }

    int totalRequired = effectivelyRequiredActivityIds.size();

    List<ActivityProgressDto> progressDtos =
        activityProgressRepository.findProgressDtosByEnrollmentId(enrollmentId);
    Map<UUID, ActivityProgressDto> progressMap =
        progressDtos.stream()
            .collect(Collectors.toMap(ActivityProgressDto::getActivityId, p -> p, (p1, p2) -> p1));

    int completedRequired = 0;
    for (UUID actId : effectivelyRequiredActivityIds) {
      ActivityProgressDto p = progressMap.get(actId);
      if (p != null && p.getStatus() == ActivityStatus.COMPLETED) {
        completedRequired++;
      }
    }

    BigDecimal progressPercent = ProgressPolicy.percentOrNull(completedRequired, totalRequired);
    return new ProgressStats(completedRequired, totalRequired, progressPercent);
  }
}
