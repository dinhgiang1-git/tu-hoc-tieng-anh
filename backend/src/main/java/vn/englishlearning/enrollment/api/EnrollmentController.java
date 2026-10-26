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
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

  private final CreateEnrollmentService createEnrollmentService;
  private final EnrollmentQueryService enrollmentQueryService;

  public EnrollmentController(
      CreateEnrollmentService createEnrollmentService,
      EnrollmentQueryService enrollmentQueryService) {
    this.createEnrollmentService = createEnrollmentService;
    this.enrollmentQueryService = enrollmentQueryService;
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
}
