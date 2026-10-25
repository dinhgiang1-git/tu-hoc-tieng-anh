package vn.englishlearning.learning.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.learning.api.LearningProgressRequests.SubmitResponseRequest;
import vn.englishlearning.learning.api.LearningProgressRequests.UpdateProgressRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.LessonActivitiesResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressResponse;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressSummaryResponse;
import vn.englishlearning.learning.application.LearningProgressService;

@RestController
@RequestMapping("/api/v1/enrollments")
public class LearningProgressController {

  private final LearningProgressService learningProgressService;

  public LearningProgressController(LearningProgressService learningProgressService) {
    this.learningProgressService = learningProgressService;
  }

  @GetMapping("/{enrollmentId}/summary")
  public ProgressSummaryResponse getSummary(@PathVariable UUID enrollmentId) {
    return learningProgressService.getSummary(enrollmentId);
  }

  @GetMapping("/{enrollmentId}/lessons/{lessonId}/activities")
  public LessonActivitiesResponse getLessonActivities(
      @PathVariable UUID enrollmentId, @PathVariable UUID lessonId) {
    return learningProgressService.getLessonActivities(enrollmentId, lessonId);
  }

  @PutMapping("/{enrollmentId}/activities/{activityId}/progress")
  public ProgressResponse updateProgress(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID activityId,
      @Valid @RequestBody UpdateProgressRequest request) {
    return learningProgressService.updateProgress(enrollmentId, activityId, request);
  }

  @PostMapping("/{enrollmentId}/activities/{activityId}/response/submit")
  public ProgressResponse submitResponse(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID activityId,
      @Valid @RequestBody SubmitResponseRequest request) {
    return learningProgressService.submitResponse(enrollmentId, activityId, request);
  }
}
