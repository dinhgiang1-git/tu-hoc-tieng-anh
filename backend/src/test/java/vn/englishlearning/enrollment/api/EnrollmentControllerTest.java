package vn.englishlearning.enrollment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.enrollment.application.CreateEnrollmentService;
import vn.englishlearning.enrollment.application.EnrollmentQueryService;
import vn.englishlearning.enrollment.application.RescheduleService;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class EnrollmentControllerTest {

  private CreateEnrollmentService createEnrollmentService;
  private EnrollmentQueryService enrollmentQueryService;
  private RescheduleService rescheduleService;
  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    createEnrollmentService = mock(CreateEnrollmentService.class);
    enrollmentQueryService = mock(EnrollmentQueryService.class);
    rescheduleService = mock(RescheduleService.class);
    EnrollmentController controller =
        new EnrollmentController(
            createEnrollmentService, enrollmentQueryService, rescheduleService);
    objectMapper =
        org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                    objectMapper))
            .build();
  }

  @Test
  @DisplayName("POST /api/v1/enrollments creates enrollment and returns 201")
  void create_returnsCreated() throws Exception {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    UUID enrollmentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String key = UUID.randomUUID().toString();

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            30,
            "Asia/Ho_Chi_Minh");

    EnrollmentResponses.EnrollmentResponse response =
        new EnrollmentResponses.EnrollmentResponse(
            enrollmentId,
            userId,
            curriculumId,
            revisionId,
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            1L);

    when(createEnrollmentService.createEnrollment(eq(key), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments returns 400 when Idempotency-Key header is missing")
  void create_missingIdempotencyKey_returns400() throws Exception {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();

    EnrollmentRequests.CreateEnrollmentRequest request =
        new EnrollmentRequests.CreateEnrollmentRequest(
            curriculumId,
            revisionId,
            LocalDate.of(2026, 10, 26),
            List.of(DayOfWeek.MONDAY),
            30,
            "Asia/Ho_Chi_Minh");

    mockMvc
        .perform(
            post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("GET /api/v1/enrollments/{id} returns 200 with detail")
  void getDetail_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();

    EnrollmentResponses.EnrollmentDetailResponse detail =
        new EnrollmentResponses.EnrollmentDetailResponse(
            enrollmentId,
            userId,
            curriculumId,
            revisionId,
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            30,
            1L,
            LocalDate.of(2026, 11, 20));

    when(enrollmentQueryService.getDetail(enrollmentId)).thenReturn(detail);

    mockMvc
        .perform(get("/api/v1/enrollments/{enrollmentId}", enrollmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.dailyTargetMinutes").value(30))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.estimatedEndDate").value("2026-11-20"));
  }

  @Test
  @DisplayName("GET /api/v1/enrollments/{id} returns 404 when not found or unauthorized")
  void getDetail_notFound_returns404() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    when(enrollmentQueryService.getDetail(enrollmentId))
        .thenThrow(new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    mockMvc
        .perform(get("/api/v1/enrollments/{enrollmentId}", enrollmentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  @DisplayName("GET /api/v1/me/enrollments returns 200 list")
  void getMyEnrollments_returnsOkList() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    EnrollmentResponses.EnrollmentSummaryResponse summary =
        new EnrollmentResponses.EnrollmentSummaryResponse(
            enrollmentId,
            "Pilot Course",
            "1.0.0",
            EnrollmentStatus.ACTIVE,
            2,
            19,
            new BigDecimal("10.5"),
            1L);

    when(enrollmentQueryService.getMyEnrollments()).thenReturn(List.of(summary));

    mockMvc
        .perform(get("/api/v1/me/enrollments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(enrollmentId.toString()))
        .andExpect(jsonPath("$[0].curriculumTitle").value("Pilot Course"))
        .andExpect(jsonPath("$[0].totalRequiredActivities").value(19))
        .andExpect(jsonPath("$[0].completedRequiredActivities").value(2))
        .andExpect(jsonPath("$[0].progressPercent").value(10.5));
  }

  @Test
  @DisplayName("GET /api/v1/enrollments/{id}/schedule returns 200 with list of schedules")
  void getSchedule_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    LocalDate plannedDate = LocalDate.of(2026, 10, 26);

    EnrollmentResponses.LessonScheduleResponse item =
        new EnrollmentResponses.LessonScheduleResponse(
            lessonId, "Bài 1: Làm quen", plannedDate, plannedDate, false, false);

    when(enrollmentQueryService.getSchedule(eq(enrollmentId), eq(plannedDate), any()))
        .thenReturn(List.of(item));

    mockMvc
        .perform(
            get("/api/v1/enrollments/{enrollmentId}/schedule", enrollmentId)
                .param("from", "2026-10-26")
                .param("to", "2026-11-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
        .andExpect(jsonPath("$[0].lessonTitle").value("Bài 1: Làm quen"))
        .andExpect(jsonPath("$[0].plannedDate").value("2026-10-26"))
        .andExpect(jsonPath("$[0].originalDate").value("2026-10-26"))
        .andExpect(jsonPath("$[0].isCompleted").value(false))
        .andExpect(jsonPath("$[0].isOverdue").value(false));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/schedule/preview returns 200 with preview response")
  void previewReschedule_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    LocalDate prevDate = LocalDate.of(2026, 10, 26);
    LocalDate newDate = LocalDate.of(2026, 10, 28);

    EnrollmentRequests.SchedulePreviewRequest request =
        new EnrollmentRequests.SchedulePreviewRequest(
            prevDate, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 30);

    EnrollmentResponses.RescheduledLessonItemResponse item =
        new EnrollmentResponses.RescheduledLessonItemResponse(
            lessonId, "Bài 1", prevDate, newDate, prevDate);

    EnrollmentResponses.SchedulePreviewResponse response =
        new EnrollmentResponses.SchedulePreviewResponse(2L, newDate, List.of(item));

    when(rescheduleService.previewReschedule(eq(enrollmentId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/schedule/preview", enrollmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enrollmentVersion").value(2))
        .andExpect(jsonPath("$.proposedEndDate").value("2026-10-28"))
        .andExpect(jsonPath("$.modifiedSchedules[0].lessonId").value(lessonId.toString()))
        .andExpect(jsonPath("$.modifiedSchedules[0].previousPlannedDate").value("2026-10-26"))
        .andExpect(jsonPath("$.modifiedSchedules[0].newPlannedDate").value("2026-10-28"));
  }

  @Test
  @DisplayName(
      "POST /api/v1/enrollments/{id}/schedule/commit requires Idempotency-Key and returns 200")
  void commitReschedule_success() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String key = UUID.randomUUID().toString();

    EnrollmentRequests.ScheduleCommitRequest request =
        new EnrollmentRequests.ScheduleCommitRequest(
            LocalDate.of(2026, 10, 26), List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 30, 2L);

    EnrollmentResponses.EnrollmentResponse response =
        new EnrollmentResponses.EnrollmentResponse(
            enrollmentId,
            userId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            3L);

    when(rescheduleService.commitReschedule(eq(key), eq(enrollmentId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/schedule/commit", enrollmentId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.version").value(3));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/schedule/commit without Idempotency-Key returns 400")
  void commitReschedule_missingKey_returnsBadRequest() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    EnrollmentRequests.ScheduleCommitRequest request =
        new EnrollmentRequests.ScheduleCommitRequest(
            LocalDate.of(2026, 10, 26), List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 30, 2L);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/schedule/commit", enrollmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/pause returns 200 with PAUSED")
  void pauseEnrollment_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    EnrollmentRequests.LifecycleTransitionRequest request =
        new EnrollmentRequests.LifecycleTransitionRequest(2L);

    EnrollmentResponses.EnrollmentResponse response =
        new EnrollmentResponses.EnrollmentResponse(
            enrollmentId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            EnrollmentStatus.PAUSED,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            3L);

    when(rescheduleService.pauseEnrollment(eq(enrollmentId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/pause", enrollmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAUSED"))
        .andExpect(jsonPath("$.version").value(3));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/resume returns 200 with ACTIVE")
  void resumeEnrollment_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    String key = UUID.randomUUID().toString();

    EnrollmentRequests.ScheduleCommitRequest request =
        new EnrollmentRequests.ScheduleCommitRequest(
            LocalDate.of(2026, 10, 26), List.of(DayOfWeek.MONDAY), 30, 3L);

    EnrollmentResponses.EnrollmentResponse response =
        new EnrollmentResponses.EnrollmentResponse(
            enrollmentId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            EnrollmentStatus.ACTIVE,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            4L);

    when(rescheduleService.resumeEnrollment(eq(key), eq(enrollmentId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/resume", enrollmentId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.version").value(4));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/abandon returns 200 with ABANDONED")
  void abandonEnrollment_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    EnrollmentRequests.LifecycleTransitionRequest request =
        new EnrollmentRequests.LifecycleTransitionRequest(4L);

    EnrollmentResponses.EnrollmentResponse response =
        new EnrollmentResponses.EnrollmentResponse(
            enrollmentId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            EnrollmentStatus.ABANDONED,
            LocalDate.of(2026, 10, 26),
            "Asia/Ho_Chi_Minh",
            List.of(DayOfWeek.MONDAY),
            5L);

    when(rescheduleService.abandonEnrollment(eq(enrollmentId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/enrollments/{enrollmentId}/abandon", enrollmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ABANDONED"))
        .andExpect(jsonPath("$.version").value(5));
  }
}
