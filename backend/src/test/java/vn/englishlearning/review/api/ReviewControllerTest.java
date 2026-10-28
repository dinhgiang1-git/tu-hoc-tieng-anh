package vn.englishlearning.review.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.assessment.api.QuizResponses.QuizOptionDto;
import vn.englishlearning.review.api.ReviewRequests.CheckQuestionRequest;
import vn.englishlearning.review.api.ReviewRequests.CreateBookmarkRequest;
import vn.englishlearning.review.api.ReviewResponses.BookmarkResponse;
import vn.englishlearning.review.api.ReviewResponses.MistakeItemResponse;
import vn.englishlearning.review.api.ReviewResponses.QuestionCheckResponse;
import vn.englishlearning.review.api.ReviewResponses.ReviewSummaryResponse;
import vn.englishlearning.review.application.ReviewService;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.security.CurrentUser;

class ReviewControllerTest {

  private ReviewService reviewService;
  private CurrentUser currentUser;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UUID userId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    reviewService = mock(ReviewService.class);
    currentUser = mock(CurrentUser.class);
    when(currentUser.requireUserId()).thenReturn(userId);

    ReviewController controller = new ReviewController(reviewService, currentUser);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/enrollments/{enrollmentId}/review returns 200 with review summary")
  void getReviewSummary_success() throws Exception {
    UUID bookmarkId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();

    ReviewSummaryResponse response =
        new ReviewSummaryResponse(
            enrollmentId,
            List.of(
                new BookmarkResponse(
                    bookmarkId,
                    enrollmentId,
                    lessonId,
                    "Lesson 1",
                    null,
                    null,
                    null,
                    "Need review",
                    Instant.now())),
            List.of(
                new MistakeItemResponse(
                    questionId,
                    "Question prompt?",
                    lessonId,
                    "Lesson 1",
                    UUID.randomUUID(),
                    "Quiz 1",
                    List.of(new QuizOptionDto(UUID.randomUUID(), 1, "A")),
                    false,
                    Instant.now())));

    when(reviewService.getReviewSummary(userId, enrollmentId)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/v1/enrollments/{enrollmentId}/review", enrollmentId)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enrollmentId").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.bookmarkedItems[0].id").value(bookmarkId.toString()))
        .andExpect(jsonPath("$.bookmarkedItems[0].lessonTitle").value("Lesson 1"))
        .andExpect(jsonPath("$.mistakes[0].questionId").value(questionId.toString()))
        .andExpect(jsonPath("$.mistakes[0].prompt").value("Question prompt?"))
        .andExpect(jsonPath("$.mistakes[0].resolved").value(false));
  }

  @Test
  @DisplayName("POST /api/v1/review-bookmarks returns 201 with created bookmark")
  void createBookmark_success() throws Exception {
    UUID lessonId = UUID.randomUUID();
    CreateBookmarkRequest request =
        new CreateBookmarkRequest(enrollmentId, lessonId, null, "Review later");

    BookmarkResponse response =
        new BookmarkResponse(
            UUID.randomUUID(),
            enrollmentId,
            lessonId,
            "Lesson 1",
            null,
            null,
            null,
            "Review later",
            Instant.now());

    when(reviewService.createBookmark(eq(userId), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/review-bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.enrollmentId").value(enrollmentId.toString()))
        .andExpect(jsonPath("$.lessonTitle").value("Lesson 1"));
  }

  @Test
  @DisplayName("DELETE /api/v1/review-bookmarks/{id} returns 204")
  void deleteBookmark_success() throws Exception {
    UUID bookmarkId = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/v1/review-bookmarks/{id}", bookmarkId))
        .andExpect(status().isNoContent());

    verify(reviewService).deleteBookmark(userId, bookmarkId);
  }

  @Test
  @DisplayName(
      "POST /api/v1/enrollments/{enrollmentId}/review/questions/{questionId}/checks returns 200")
  void checkMistakeQuestion_success() throws Exception {
    UUID questionId = UUID.randomUUID();
    UUID selectedOptionId = UUID.randomUUID();
    UUID correctOptionId = UUID.randomUUID();

    CheckQuestionRequest request = new CheckQuestionRequest(selectedOptionId);

    QuestionCheckResponse response =
        new QuestionCheckResponse(
            questionId, selectedOptionId, correctOptionId, true, "Explanation text", true);

    when(reviewService.checkMistakeQuestion(eq(userId), eq(enrollmentId), eq(questionId), any()))
        .thenReturn(response);

    mockMvc
        .perform(
            post(
                    "/api/v1/enrollments/{enrollmentId}/review/questions/{questionId}/checks",
                    enrollmentId,
                    questionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questionId").value(questionId.toString()))
        .andExpect(jsonPath("$.isCorrect").value(true))
        .andExpect(jsonPath("$.resolved").value(true))
        .andExpect(jsonPath("$.explanation").value("Explanation text"));
  }
}
