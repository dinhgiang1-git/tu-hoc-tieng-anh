package vn.englishlearning.assessment.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.assessment.api.QuizRequests.QuizSubmitRequest;
import vn.englishlearning.assessment.api.QuizResponses.QuizDetailResponse;
import vn.englishlearning.assessment.api.QuizResponses.QuizResultResponse;
import vn.englishlearning.assessment.application.QuizService;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/activities/{activityId}/quiz")
public class QuizController {

  private final QuizService quizService;

  public QuizController(QuizService quizService) {
    this.quizService = quizService;
  }

  @GetMapping
  public ResponseEntity<QuizDetailResponse> getQuiz(
      @PathVariable UUID enrollmentId, @PathVariable UUID activityId) {
    QuizDetailResponse response = quizService.getQuizDetail(enrollmentId, activityId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/submit")
  public ResponseEntity<QuizResultResponse> submitQuiz(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID activityId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody QuizSubmitRequest request) {
    QuizResultResponse response =
        quizService.submitQuiz(idempotencyKey, enrollmentId, activityId, request);
    return ResponseEntity.ok(response);
  }
}
