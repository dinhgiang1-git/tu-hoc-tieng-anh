package vn.englishlearning.review.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.review.api.ReviewRequests.CheckQuestionRequest;
import vn.englishlearning.review.api.ReviewRequests.CreateBookmarkRequest;
import vn.englishlearning.review.api.ReviewResponses.BookmarkResponse;
import vn.englishlearning.review.api.ReviewResponses.QuestionCheckResponse;
import vn.englishlearning.review.api.ReviewResponses.ReviewSummaryResponse;
import vn.englishlearning.review.application.ReviewService;
import vn.englishlearning.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

  private final ReviewService reviewService;
  private final CurrentUser currentUser;

  public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
    this.reviewService = reviewService;
    this.currentUser = currentUser;
  }

  @GetMapping("/enrollments/{enrollmentId}/review")
  public ResponseEntity<ReviewSummaryResponse> getReviewSummary(@PathVariable UUID enrollmentId) {
    UUID userId = currentUser.requireUserId();
    ReviewSummaryResponse response = reviewService.getReviewSummary(userId, enrollmentId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/review-bookmarks")
  public ResponseEntity<BookmarkResponse> createBookmark(
      @Valid @RequestBody CreateBookmarkRequest request) {
    UUID userId = currentUser.requireUserId();
    BookmarkResponse response = reviewService.createBookmark(userId, request);
    return ResponseEntity.created(URI.create("/api/v1/review-bookmarks/" + response.id()))
        .body(response);
  }

  @DeleteMapping("/review-bookmarks/{id}")
  public ResponseEntity<Void> deleteBookmark(@PathVariable UUID id) {
    UUID userId = currentUser.requireUserId();
    reviewService.deleteBookmark(userId, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/enrollments/{enrollmentId}/review/questions/{questionId}/checks")
  public ResponseEntity<QuestionCheckResponse> checkMistakeQuestion(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID questionId,
      @Valid @RequestBody CheckQuestionRequest request) {
    UUID userId = currentUser.requireUserId();
    QuestionCheckResponse response =
        reviewService.checkMistakeQuestion(userId, enrollmentId, questionId, request);
    return ResponseEntity.ok(response);
  }
}
