package vn.englishlearning.learning.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.learning.application.TodayQueryService;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class TodayControllerTest {

  private TodayQueryService todayQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    todayQueryService = mock(TodayQueryService.class);
    TodayController controller = new TodayController(todayQueryService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/me/today returns 200 with TodayResponse")
  void getToday_returnsOk() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();

    TodayResponses.TodayResponse response =
        new TodayResponses.TodayResponse(
            "IN_PROGRESS",
            enrollmentId,
            revisionId,
            "Pilot Course",
            LocalDate.of(2026, 10, 28),
            false,
            2L,
            new TodayResponses.PrimaryActionDto(
                "IN_PROGRESS", "Bạn đang học dở Buổi 2", lessonId, "Buổi 2", 20, 1, 4),
            List.of(),
            new TodayResponses.ProgressSummaryResponse(1, 19, new BigDecimal("5.3"), 2L));

    when(todayQueryService.getToday(null)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/me/today"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.enrollmentId").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.primaryAction.lessonId").value(lessonId.toString()))
        .andExpect(jsonPath("$.enrollmentVersion").value(2))
        .andExpect(jsonPath("$.progress.totalRequiredActivities").value(19));
  }

  @Test
  @DisplayName("GET /api/v1/me/today?enrollmentId=... passes query param to service")
  void getToday_withEnrollmentId_passesParam() throws Exception {
    UUID enrollmentId = UUID.randomUUID();

    TodayResponses.TodayResponse response =
        new TodayResponses.TodayResponse(
            "NO_ENROLLMENT",
            null,
            null,
            null,
            LocalDate.of(2026, 10, 28),
            false,
            null,
            null,
            List.of(),
            null);

    when(todayQueryService.getToday(enrollmentId)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/me/today").param("enrollmentId", enrollmentId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("NO_ENROLLMENT"));
  }

  @Test
  @DisplayName("GET /api/v1/me/today returns 404 when enrollment belongs to another user")
  void getToday_unauthorizedEnrollment_returns404() throws Exception {
    UUID enrollmentId = UUID.randomUUID();
    when(todayQueryService.getToday(enrollmentId))
        .thenThrow(new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "Enrollment not found"));

    mockMvc
        .perform(get("/api/v1/me/today").param("enrollmentId", enrollmentId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }
}
