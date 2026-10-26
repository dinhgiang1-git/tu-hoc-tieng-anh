package vn.englishlearning.learning.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
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
import vn.englishlearning.learning.application.LearningProgressService;
import vn.englishlearning.learning.domain.ActivityStatus;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class LearningProgressControllerTest {

  private LearningProgressService service;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID lessonId = UUID.randomUUID();
  private final UUID activityId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = mock(LearningProgressService.class);
    LearningProgressController controller = new LearningProgressController(service);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/enrollments/{id}/summary returns 200 with ProgressSummaryResponse")
  void getSummary_returnsOk() throws Exception {
    ProgressSummaryResponse response =
        new ProgressSummaryResponse(5, 10, new BigDecimal("50.0"), 3L);
    when(service.getSummary(enrollmentId)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/enrollments/{enrollmentId}/summary", enrollmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completedRequiredActivities").value(5))
        .andExpect(jsonPath("$.totalRequiredActivities").value(10))
        .andExpect(jsonPath("$.progressPercent").value(50.0))
        .andExpect(jsonPath("$.enrollmentVersion").value(3));
  }

  @Test
  @DisplayName(
      "GET /api/v1/enrollments/{id}/lessons/{lessonId}/activities returns 200 with activity list")
  void getLessonActivities_returnsOk() throws Exception {
    LessonActivityItemDto item =
        new LessonActivityItemDto(
            activityId,
            "Activity 1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            ActivityStatus.COMPLETED,
            null);
    LessonActivitiesResponse response = new LessonActivitiesResponse(lessonId, 2L, List.of(item));
    when(service.getLessonActivities(enrollmentId, lessonId)).thenReturn(response);

    mockMvc
        .perform(
            get(
                "/api/v1/enrollments/{enrollmentId}/lessons/{lessonId}/activities",
                enrollmentId,
                lessonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
        .andExpect(jsonPath("$.enrollmentVersion").value(2))
        .andExpect(jsonPath("$.activities[0].id").value(activityId.toString()))
        .andExpect(jsonPath("$.activities[0].status").value("COMPLETED"));
  }

  @Test
  @DisplayName("PUT /api/v1/enrollments/{id}/activities/{activityId}/progress returns 200")
  void updateProgress_returnsOk() throws Exception {
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 1L);
    ProgressResponse response =
        new ProgressResponse(
            activityId, ActivityStatus.COMPLETED, 2L, 6, 10, new BigDecimal("60.0"));
    when(service.updateProgress(eq(enrollmentId), eq(activityId), any())).thenReturn(response);

    mockMvc
        .perform(
            put(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/progress",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityId").value(activityId.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.enrollmentVersion").value(2))
        .andExpect(jsonPath("$.completedRequiredActivities").value(6));
  }

  @Test
  @DisplayName("PUT progress with invalid body returns 400 VALIDATION_FAILED")
  void updateProgress_invalidBody_returns400() throws Exception {
    mockMvc
        .perform(
            put(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/progress",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("POST /api/v1/enrollments/{id}/activities/{activityId}/response/submit returns 200")
  void submitResponse_returnsOk() throws Exception {
    SubmitResponseRequest request = new SubmitResponseRequest("My reflective answer text", 1L);
    ProgressResponse response =
        new ProgressResponse(
            activityId, ActivityStatus.COMPLETED, 2L, 7, 10, new BigDecimal("70.0"));
    when(service.submitResponse(eq(enrollmentId), eq(activityId), any())).thenReturn(response);

    mockMvc
        .perform(
            post(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/response/submit",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityId").value(activityId.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.enrollmentVersion").value(2));
  }

  @Test
  @DisplayName("POST submit response with empty text returns 400 VALIDATION_FAILED")
  void submitResponse_emptyText_returns400() throws Exception {
    SubmitResponseRequest request = new SubmitResponseRequest("", 1L);

    mockMvc
        .perform(
            post(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/response/submit",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("PUT progress with stale version returns 409 VERSION_CONFLICT")
  void updateProgress_versionConflict_returns409() throws Exception {
    UpdateProgressRequest request = new UpdateProgressRequest(ActivityStatus.COMPLETED, 0L);
    when(service.updateProgress(eq(enrollmentId), eq(activityId), any()))
        .thenThrow(
            new DomainException(
                ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại trước khi lưu."));

    mockMvc
        .perform(
            put(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/progress",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  }

  @Test
  @DisplayName("GET summary on unauthorized enrollment returns 404 RESOURCE_NOT_FOUND")
  void getSummary_notFound_returns404() throws Exception {
    when(service.getSummary(enrollmentId))
        .thenThrow(new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    mockMvc
        .perform(get("/api/v1/enrollments/{enrollmentId}/summary", enrollmentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/enrollments/{enrollmentId}/lessons/{lessonId} returns 200 with LessonDetailResponse")
  void getLessonWorkspace_returnsOk() throws Exception {
    ResourceDto resource =
        new ResourceDto(
            UUID.randomUUID(),
            "Resource 1",
            "https://example.com/res",
            "ARTICLE",
            "Example Attribution");
    ActivityWorkspaceDto activityDto =
        new ActivityWorkspaceDto(
            activityId,
            "Activity 1",
            ActivityType.READING,
            true,
            CompletionPolicy.MANUAL_CHECK,
            "Read carefully",
            ActivityStatus.NOT_STARTED,
            null,
            List.of(resource));
    LessonDetailResponse response =
        new LessonDetailResponse(
            lessonId, "Lesson 1", 30, true, "# Lesson Markdown", 1L, null, List.of(activityDto));

    when(service.getLessonWorkspace(enrollmentId, lessonId)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/enrollments/{enrollmentId}/lessons/{lessonId}", enrollmentId, lessonId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
        .andExpect(jsonPath("$.title").value("Lesson 1"))
        .andExpect(jsonPath("$.markdownContent").value("# Lesson Markdown"))
        .andExpect(jsonPath("$.activities[0].activityId").value(activityId.toString()))
        .andExpect(jsonPath("$.activities[0].resources[0].title").value("Resource 1"));
  }

  @Test
  @DisplayName(
      "PUT /api/v1/enrollments/{enrollmentId}/activities/{activityId}/response returns 200 with ActivityResponseDto")
  void saveResponseDraft_returnsOk() throws Exception {
    SaveResponseDraftRequest request = new SaveResponseDraftRequest("My draft answer", 1L);
    ActivityResponseDto response =
        new ActivityResponseDto(
            UUID.randomUUID(),
            activityId,
            "My draft answer",
            Instant.parse("2026-10-26T10:00:00Z"),
            2L);

    when(service.saveResponseDraft(
            eq(enrollmentId), eq(activityId), any(SaveResponseDraftRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            put(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/response",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityId").value(activityId.toString()))
        .andExpect(jsonPath("$.responseText").value("My draft answer"))
        .andExpect(jsonPath("$.enrollmentVersion").value(2));
  }
}
