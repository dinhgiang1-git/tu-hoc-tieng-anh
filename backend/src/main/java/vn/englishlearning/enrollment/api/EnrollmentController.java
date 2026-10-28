package vn.englishlearning.enrollment.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.enrollment.application.CreateEnrollmentService;
import vn.englishlearning.enrollment.application.EnrollmentQueryService;
import vn.englishlearning.enrollment.application.RescheduleService;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

  private final CreateEnrollmentService createEnrollmentService;
  private final EnrollmentQueryService enrollmentQueryService;
  private final RescheduleService rescheduleService;

  public EnrollmentController(
      CreateEnrollmentService createEnrollmentService,
      EnrollmentQueryService enrollmentQueryService,
      RescheduleService rescheduleService) {
    this.createEnrollmentService = createEnrollmentService;
    this.enrollmentQueryService = enrollmentQueryService;
    this.rescheduleService = rescheduleService;
  }

  @PostMapping("/enrollments")
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponses.EnrollmentResponse create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody EnrollmentRequests.CreateEnrollmentRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
    }
    return createEnrollmentService.createEnrollment(idempotencyKey, request);
  }

  @GetMapping("/enrollments/{enrollmentId}")
  public EnrollmentResponses.EnrollmentDetailResponse getDetail(@PathVariable UUID enrollmentId) {
    return enrollmentQueryService.getDetail(enrollmentId);
  }

  @GetMapping("/enrollments/{enrollmentId}/schedule")
  public List<EnrollmentResponses.LessonScheduleResponse> getSchedule(
      @PathVariable UUID enrollmentId,
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to) {
    return enrollmentQueryService.getSchedule(enrollmentId, from, to);
  }

  @GetMapping("/enrollments/{enrollmentId}/lessons/{lessonId}/summary")
  public EnrollmentResponses.LessonSummaryResponse getLessonSummary(
      @PathVariable UUID enrollmentId, @PathVariable UUID lessonId) {
    return enrollmentQueryService.getLessonSummary(enrollmentId, lessonId);
  }

  @GetMapping("/me/enrollments")
  public List<EnrollmentResponses.EnrollmentSummaryResponse> getMyEnrollments() {
    return enrollmentQueryService.getMyEnrollments();
  }

  @PostMapping("/enrollments/{enrollmentId}/schedule/preview")
  public EnrollmentResponses.SchedulePreviewResponse previewReschedule(
      @PathVariable UUID enrollmentId,
      @Valid @RequestBody EnrollmentRequests.SchedulePreviewRequest request) {
    return rescheduleService.previewReschedule(enrollmentId, request);
  }

  @PostMapping("/enrollments/{enrollmentId}/schedule/commit")
  public EnrollmentResponses.EnrollmentResponse commitReschedule(
      @PathVariable UUID enrollmentId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody EnrollmentRequests.ScheduleCommitRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
    }
    return rescheduleService.commitReschedule(idempotencyKey, enrollmentId, request);
  }

  @PostMapping("/enrollments/{enrollmentId}/pause")
  public EnrollmentResponses.EnrollmentResponse pauseEnrollment(
      @PathVariable UUID enrollmentId,
      @Valid @RequestBody EnrollmentRequests.LifecycleTransitionRequest request) {
    return rescheduleService.pauseEnrollment(enrollmentId, request);
  }

  @PostMapping("/enrollments/{enrollmentId}/resume")
  public EnrollmentResponses.EnrollmentResponse resumeEnrollment(
      @PathVariable UUID enrollmentId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody EnrollmentRequests.ScheduleCommitRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
    }
    return rescheduleService.resumeEnrollment(idempotencyKey, enrollmentId, request);
  }

  @PostMapping("/enrollments/{enrollmentId}/abandon")
  public EnrollmentResponses.EnrollmentResponse abandonEnrollment(
      @PathVariable UUID enrollmentId,
      @Valid @RequestBody EnrollmentRequests.LifecycleTransitionRequest request) {
    return rescheduleService.abandonEnrollment(enrollmentId, request);
  }
}
