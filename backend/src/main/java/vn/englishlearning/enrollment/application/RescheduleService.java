package vn.englishlearning.enrollment.application;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.enrollment.api.EnrollmentRequests.LifecycleTransitionRequest;
import vn.englishlearning.enrollment.api.EnrollmentRequests.ScheduleCommitRequest;
import vn.englishlearning.enrollment.api.EnrollmentRequests.SchedulePreviewRequest;
import vn.englishlearning.enrollment.api.EnrollmentResponses.EnrollmentResponse;
import vn.englishlearning.enrollment.api.EnrollmentResponses.RescheduledLessonItemResponse;
import vn.englishlearning.enrollment.api.EnrollmentResponses.SchedulePreviewResponse;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleDetailDto;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.scheduling.domain.PlannedLesson;
import vn.englishlearning.scheduling.domain.SchedulableLesson;
import vn.englishlearning.scheduling.domain.SchedulePlan;
import vn.englishlearning.scheduling.domain.SchedulePlanner;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.application.IdempotencyService;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.security.CurrentUser;

@Service
public class RescheduleService {

  private final EnrollmentRepository enrollmentRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final AppUserRepository userRepository;
  private final IdempotencyService idempotencyService;
  private final CurrentUser currentUser;
  private final EntityManager entityManager;
  private final Clock clock;

  public RescheduleService(
      EnrollmentRepository enrollmentRepository,
      LessonScheduleRepository lessonScheduleRepository,
      AppUserRepository userRepository,
      IdempotencyService idempotencyService,
      CurrentUser currentUser,
      EntityManager entityManager,
      Clock clock) {
    this.enrollmentRepository = enrollmentRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.userRepository = userRepository;
    this.idempotencyService = idempotencyService;
    this.currentUser = currentUser;
    this.entityManager = entityManager;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public SchedulePreviewResponse previewReschedule(
      UUID enrollmentId, SchedulePreviewRequest request) {
    UUID userId = currentUser.requireUserId();
    validateCommonInputs(request.resumeDate(), request.studyDays(), request.dailyTargetMinutes());

    Enrollment enrollment = findAndValidateEnrollment(enrollmentId, userId);
    validateResumeDateNotBeforeToday(request.resumeDate(), enrollment.getTimeZone());

    List<DayOfWeek> normalizedDays = normalizeDays(request.studyDays());
    ReschedulePlanResult planResult =
        calculateReschedulePlan(enrollmentId, request.resumeDate(), normalizedDays);

    return new SchedulePreviewResponse(
        enrollment.getVersion(), planResult.proposedEndDate(), planResult.modifiedSchedules());
  }

  @Transactional
  public EnrollmentResponse commitReschedule(
      String rawIdempotencyKey, UUID enrollmentId, ScheduleCommitRequest request) {
    UUID userId = currentUser.requireUserId();
    String idempotencyKey = idempotencyService.validateAndNormalizeKey(rawIdempotencyKey);

    if (request == null || request.expectedVersion() == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    if (request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "expectedVersion phải không âm.");
    }
    validateCommonInputs(request.resumeDate(), request.studyDays(), request.dailyTargetMinutes());

    List<DayOfWeek> normalizedDays = normalizeDays(request.studyDays());
    String normalizedDaysStr =
        normalizedDays.stream().map(DayOfWeek::name).collect(Collectors.joining(","));

    String requestHash =
        idempotencyService.hashPayload(
            enrollmentId
                + "|"
                + request.resumeDate()
                + "|"
                + normalizedDaysStr
                + "|"
                + request.dailyTargetMinutes()
                + "|"
                + request.expectedVersion());

    // 1. Lock user row for concurrency control within transaction
    userRepository.findByIdForUpdate(userId);

    // 2. Replay lookup BEFORE version/lifecycle/temporal checks
    Optional<IdempotencyRecord> existing =
        idempotencyService.findExisting(userId, "COMMIT_RESCHEDULE", idempotencyKey);
    if (existing.isPresent()) {
      return idempotencyService.replay(existing.get(), requestHash, EnrollmentResponse.class);
    }

    // 3. New request: validate enrollment and version
    Enrollment enrollment = findAndValidateEnrollment(enrollmentId, userId);

    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    validateResumeDateNotBeforeToday(request.resumeDate(), enrollment.getTimeZone());

    // 4. Calculate new plan
    ReschedulePlanResult planResult =
        calculateReschedulePlan(enrollmentId, request.resumeDate(), normalizedDays);

    // 5. Check true no-op
    boolean noSchedulesChanged = planResult.modifiedSchedules().isEmpty();
    boolean studyDaysUnchanged = Objects.equals(enrollment.getStudyDays(), normalizedDaysStr);
    boolean targetMinutesUnchanged =
        enrollment.getDailyTargetMinutes() == request.dailyTargetMinutes();

    if (noSchedulesChanged && studyDaysUnchanged && targetMinutesUnchanged) {
      EnrollmentResponse noopResponse =
          toEnrollmentResponse(enrollment, enrollment.getVersion(), normalizedDays);
      idempotencyService.record(
          userId, "COMMIT_RESCHEDULE", idempotencyKey, requestHash, 200, noopResponse);
      return noopResponse;
    }

    // 6. Apply updates
    Instant now = Instant.now(clock);

    for (ModifiedScheduleItem item : planResult.scheduleUpdates()) {
      lessonScheduleRepository.updatePlannedDate(item.scheduleId(), item.newPlannedDate(), now);
    }

    entityManager.detach(enrollment);

    Long newVersion = request.expectedVersion() + 1;
    int updatedRows =
        enrollmentRepository.updateScheduleConfigAndVersion(
            enrollment.getId(),
            request.expectedVersion(),
            newVersion,
            normalizedDaysStr,
            request.dailyTargetMinutes(),
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    EnrollmentResponse response =
        toEnrollmentResponse(enrollment, newVersion, normalizedDays, EnrollmentStatus.ACTIVE);
    idempotencyService.record(
        userId, "COMMIT_RESCHEDULE", idempotencyKey, requestHash, 200, response);
    return response;
  }

  @Transactional
  public EnrollmentResponse pauseEnrollment(UUID enrollmentId, LifecycleTransitionRequest request) {
    UUID userId = currentUser.requireUserId();
    if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }

    userRepository.findByIdForUpdate(userId);

    Enrollment enrollment = findEnrollmentForUser(enrollmentId, userId);
    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Chỉ có thể tạm dừng khi lộ trình đang học (ACTIVE). Trạng thái hiện tại: "
              + enrollment.getStatus());
    }

    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    Instant now = Instant.now(clock);
    entityManager.detach(enrollment);

    Long newVersion = request.expectedVersion() + 1;
    int updatedRows =
        enrollmentRepository.updateStatusAndVersion(
            enrollmentId,
            request.expectedVersion(),
            newVersion,
            EnrollmentStatus.ACTIVE,
            EnrollmentStatus.PAUSED,
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    return toEnrollmentResponse(
        enrollment, newVersion, enrollment.getStudyDaysList(), EnrollmentStatus.PAUSED);
  }

  @Transactional
  public EnrollmentResponse resumeEnrollment(
      String rawIdempotencyKey, UUID enrollmentId, ScheduleCommitRequest request) {
    UUID userId = currentUser.requireUserId();
    String idempotencyKey = idempotencyService.validateAndNormalizeKey(rawIdempotencyKey);

    if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }
    validateCommonInputs(request.resumeDate(), request.studyDays(), request.dailyTargetMinutes());

    List<DayOfWeek> normalizedDays = normalizeDays(request.studyDays());
    String normalizedDaysStr =
        normalizedDays.stream().map(DayOfWeek::name).collect(Collectors.joining(","));

    String requestHash =
        idempotencyService.hashPayload(
            enrollmentId
                + "|"
                + request.resumeDate()
                + "|"
                + normalizedDaysStr
                + "|"
                + request.dailyTargetMinutes()
                + "|"
                + request.expectedVersion());

    userRepository.findByIdForUpdate(userId);

    Optional<IdempotencyRecord> existing =
        idempotencyService.findExisting(userId, "RESUME_ENROLLMENT", idempotencyKey);
    if (existing.isPresent()) {
      return idempotencyService.replay(existing.get(), requestHash, EnrollmentResponse.class);
    }

    Enrollment enrollment = findEnrollmentForUser(enrollmentId, userId);
    if (enrollment.getStatus() != EnrollmentStatus.PAUSED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Chỉ có thể tiếp tục khi lộ trình đang tạm dừng (PAUSED). Trạng thái hiện tại: "
              + enrollment.getStatus());
    }

    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    validateResumeDateNotBeforeToday(request.resumeDate(), enrollment.getTimeZone());

    ReschedulePlanResult planResult =
        calculateReschedulePlan(enrollmentId, request.resumeDate(), normalizedDays);

    Instant now = Instant.now(clock);

    for (ModifiedScheduleItem item : planResult.scheduleUpdates()) {
      lessonScheduleRepository.updatePlannedDate(item.scheduleId(), item.newPlannedDate(), now);
    }

    entityManager.detach(enrollment);

    Long newVersion = request.expectedVersion() + 1;
    int updatedRows =
        enrollmentRepository.resumeEnrollment(
            enrollmentId,
            request.expectedVersion(),
            newVersion,
            EnrollmentStatus.PAUSED,
            EnrollmentStatus.ACTIVE,
            normalizedDaysStr,
            request.dailyTargetMinutes(),
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    EnrollmentResponse response =
        toEnrollmentResponse(enrollment, newVersion, normalizedDays, EnrollmentStatus.ACTIVE);
    idempotencyService.record(
        userId, "RESUME_ENROLLMENT", idempotencyKey, requestHash, 200, response);
    return response;
  }

  @Transactional
  public EnrollmentResponse abandonEnrollment(
      UUID enrollmentId, LifecycleTransitionRequest request) {
    UUID userId = currentUser.requireUserId();
    if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Dữ liệu yêu cầu không hợp lệ.");
    }

    userRepository.findByIdForUpdate(userId);

    Enrollment enrollment = findEnrollmentForUser(enrollmentId, userId);
    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
        && enrollment.getStatus() != EnrollmentStatus.PAUSED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Chỉ có thể kết thúc khi lộ trình đang học (ACTIVE) hoặc tạm dừng (PAUSED). Trạng thái hiện tại: "
              + enrollment.getStatus());
    }

    if (!enrollment.getVersion().equals(request.expectedVersion())) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    Instant now = Instant.now(clock);
    entityManager.detach(enrollment);

    Long newVersion = request.expectedVersion() + 1;
    int updatedRows =
        enrollmentRepository.abandonEnrollment(
            enrollmentId,
            request.expectedVersion(),
            newVersion,
            List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PAUSED),
            EnrollmentStatus.ABANDONED,
            now);

    if (updatedRows == 0) {
      throw new DomainException(
          ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu.");
    }

    return toEnrollmentResponse(
        enrollment, newVersion, enrollment.getStudyDaysList(), EnrollmentStatus.ABANDONED);
  }

  private void validateCommonInputs(
      LocalDate resumeDate, List<DayOfWeek> studyDays, int dailyTargetMinutes) {
    if (resumeDate == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Ngày tiếp tục không được để trống.");
    }
    if (dailyTargetMinutes < 10 || dailyTargetMinutes > 180) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Mục tiêu phút mỗi ngày phải từ 10 đến 180 phút.");
    }
    if (studyDays == null || studyDays.isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Phải chọn ít nhất một ngày học trong tuần.");
    }
    for (DayOfWeek day : studyDays) {
      if (day == null) {
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED, "Ngày học trong tuần không được chứa giá trị rỗng.");
      }
    }
  }

  private Enrollment findEnrollmentForUser(UUID enrollmentId, UUID userId) {
    Enrollment enrollment =
        enrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId));

    if (!enrollment.getUserId().equals(userId)) {
      throw new DomainException(
          ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found: " + enrollmentId);
    }
    return enrollment;
  }

  private Enrollment findAndValidateEnrollment(UUID enrollmentId, UUID userId) {
    Enrollment enrollment = findEnrollmentForUser(enrollmentId, userId);

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE
        && enrollment.getStatus() != EnrollmentStatus.PAUSED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Chỉ có thể xem trước lịch khi lộ trình đang học (ACTIVE) hoặc tạm dừng (PAUSED). Trạng thái hiện tại: "
              + enrollment.getStatus());
    }

    return enrollment;
  }

  private void validateResumeDateNotBeforeToday(LocalDate resumeDate, String timeZone) {
    ZoneId zoneId;
    try {
      zoneId = ZoneId.of(timeZone);
    } catch (Exception ex) {
      zoneId = ZoneId.of("UTC");
    }
    LocalDate today = LocalDate.now(clock.withZone(zoneId));
    if (resumeDate.isBefore(today)) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "Ngày tiếp tục không được trước ngày hôm nay (" + today + ").");
    }
  }

  private List<DayOfWeek> normalizeDays(List<DayOfWeek> studyDays) {
    return studyDays.stream().distinct().sorted().toList();
  }

  private ReschedulePlanResult calculateReschedulePlan(
      UUID enrollmentId, LocalDate resumeDate, List<DayOfWeek> normalizedDays) {
    List<LessonScheduleDetailDto> allSchedules =
        lessonScheduleRepository.findAllScheduleDetailsByEnrollmentIdOrdered(enrollmentId);

    if (allSchedules.isEmpty()) {
      return new ReschedulePlanResult(null, List.of(), List.of());
    }

    List<LessonScheduleDetailDto> completedSchedules = new ArrayList<>();
    List<LessonScheduleDetailDto> uncompletedSchedules = new ArrayList<>();
    for (LessonScheduleDetailDto s : allSchedules) {
      if (s.completedAt() != null) {
        completedSchedules.add(s);
      } else {
        uncompletedSchedules.add(s);
      }
    }

    LocalDate maxCompletedDate =
        completedSchedules.stream()
            .map(LessonScheduleDetailDto::plannedDate)
            .max(LocalDate::compareTo)
            .orElse(null);

    if (uncompletedSchedules.isEmpty()) {
      return new ReschedulePlanResult(maxCompletedDate, List.of(), List.of());
    }

    List<SchedulableLesson> schedulableLessons =
        uncompletedSchedules.stream()
            .map(s -> new SchedulableLesson(s.lessonId(), s.isRequired()))
            .toList();

    SchedulePlan plan = SchedulePlanner.plan(schedulableLessons, resumeDate, normalizedDays);

    List<RescheduledLessonItemResponse> modifiedResponses = new ArrayList<>();
    List<ModifiedScheduleItem> scheduleUpdates = new ArrayList<>();

    Map<UUID, LocalDate> plannedDatesByLessonId =
        plan.plannedLessons().stream()
            .collect(Collectors.toMap(PlannedLesson::lessonId, PlannedLesson::plannedDate));

    for (LessonScheduleDetailDto existing : uncompletedSchedules) {
      LocalDate newDate = plannedDatesByLessonId.get(existing.lessonId());
      if (newDate != null && !newDate.equals(existing.plannedDate())) {
        modifiedResponses.add(
            new RescheduledLessonItemResponse(
                existing.lessonId(),
                existing.lessonTitle(),
                existing.plannedDate(),
                newDate,
                existing.originalDate()));
        scheduleUpdates.add(new ModifiedScheduleItem(existing.scheduleId(), newDate));
      }
    }

    LocalDate maxUncompletedDate = plan.estimatedEndDate();
    LocalDate proposedEndDate;
    if (maxCompletedDate == null) {
      proposedEndDate = maxUncompletedDate;
    } else if (maxUncompletedDate == null) {
      proposedEndDate = maxCompletedDate;
    } else {
      proposedEndDate =
          maxCompletedDate.isAfter(maxUncompletedDate) ? maxCompletedDate : maxUncompletedDate;
    }

    return new ReschedulePlanResult(proposedEndDate, modifiedResponses, scheduleUpdates);
  }

  private EnrollmentResponse toEnrollmentResponse(
      Enrollment enrollment, Long version, List<DayOfWeek> studyDays) {
    return toEnrollmentResponse(enrollment, version, studyDays, enrollment.getStatus());
  }

  private EnrollmentResponse toEnrollmentResponse(
      Enrollment enrollment, Long version, List<DayOfWeek> studyDays, EnrollmentStatus status) {
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getUserId(),
        enrollment.getCurriculumId(),
        enrollment.getRevisionId(),
        status,
        enrollment.getStartDate(),
        enrollment.getTimeZone(),
        studyDays,
        version);
  }

  record ModifiedScheduleItem(UUID scheduleId, LocalDate newPlannedDate) {}

  record ReschedulePlanResult(
      LocalDate proposedEndDate,
      List<RescheduledLessonItemResponse> modifiedSchedules,
      List<ModifiedScheduleItem> scheduleUpdates) {}
}
