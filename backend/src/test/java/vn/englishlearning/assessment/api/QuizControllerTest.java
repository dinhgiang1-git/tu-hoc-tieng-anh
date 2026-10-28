package vn.englishlearning.assessment.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.assessment.api.QuizRequests.QuizAnswerItemRequest;
import vn.englishlearning.assessment.api.QuizRequests.QuizSubmitRequest;
import vn.englishlearning.assessment.api.QuizResponses.QuestionResultDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizDetailResponse;
import vn.englishlearning.assessment.api.QuizResponses.QuizOptionDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizQuestionDto;
import vn.englishlearning.assessment.api.QuizResponses.QuizResultResponse;
import vn.englishlearning.assessment.application.QuizService;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class QuizControllerTest {

  private QuizService quizService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID activityId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    quizService = mock(QuizService.class);
    QuizController controller = new QuizController(quizService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName(
      "GET /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz returns 200 with quiz detail")
  void getQuiz_success() throws Exception {
    UUID questionId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();

    QuizDetailResponse response =
        new QuizDetailResponse(
            activityId,
            "Lesson Quiz",
            "PASS_SCORE",
            80,
            List.of(
                new QuizQuestionDto(
                    questionId,
                    1,
                    "What is polymorphism?",
                    List.of(new QuizOptionDto(optionId, 1, "Many forms")))),
            null);

    when(quizService.getQuizDetail(enrollmentId, activityId)).thenReturn(response);

    mockMvc
        .perform(
            get(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz",
                    enrollmentId,
                    activityId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activityId").value(activityId.toString()))
        .andExpect(jsonPath("$.completionPolicy").value("PASS_SCORE"))
        .andExpect(jsonPath("$.passingScorePercentage").value(80))
        .andExpect(jsonPath("$.questions[0].prompt").value("What is polymorphism?"))
        .andExpect(jsonPath("$.questions[0].options[0].content").value("Many forms"));
  }

  @Test
  @DisplayName(
      "POST /api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz/submit returns 200 with result")
  void submitQuiz_success() throws Exception {
    UUID attemptId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();

    QuizSubmitRequest request =
        new QuizSubmitRequest(1L, List.of(new QuizAnswerItemRequest(questionId, optionId)));

    QuizResultResponse response =
        new QuizResultResponse(
            attemptId,
            1,
            1,
            1,
            100,
            true,
            true,
            2L,
            List.of(
                new QuestionResultDto(
                    questionId,
                    optionId,
                    optionId,
                    true,
                    "Polymorphism allows objects to be treated as instances of their parent class.")));

    when(quizService.submitQuiz(any(), eq(enrollmentId), eq(activityId), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            post(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz/submit",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
        .andExpect(jsonPath("$.score").value(1))
        .andExpect(jsonPath("$.totalQuestions").value(1))
        .andExpect(jsonPath("$.scorePercentage").value(100))
        .andExpect(jsonPath("$.passed").value(true))
        .andExpect(jsonPath("$.activityCompleted").value(true))
        .andExpect(jsonPath("$.questionResults[0].isCorrect").value(true))
        .andExpect(
            jsonPath("$.questionResults[0].explanation")
                .value(
                    "Polymorphism allows objects to be treated as instances of their parent class."));
  }

  @Test
  @DisplayName("POST /api/v1/.../submit returns 409 when version conflict occurs")
  void submitQuiz_versionConflict() throws Exception {
    QuizSubmitRequest request =
        new QuizSubmitRequest(
            1L, List.of(new QuizAnswerItemRequest(UUID.randomUUID(), UUID.randomUUID())));

    when(quizService.submitQuiz(any(), eq(enrollmentId), eq(activityId), any()))
        .thenThrow(new DomainException(ErrorCode.VERSION_CONFLICT, "Dữ liệu đã thay đổi."));

    mockMvc
        .perform(
            post(
                    "/api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz/submit",
                    enrollmentId,
                    activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  }
}
