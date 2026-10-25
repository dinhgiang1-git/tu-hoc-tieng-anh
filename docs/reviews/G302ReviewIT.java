package vn.englishlearning.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.learning.api.LearningProgressRequests.UpdateProgressRequest;
import vn.englishlearning.learning.api.LearningProgressResponses.ProgressResponse;
import vn.englishlearning.learning.domain.ActivityStatus;

class G302ReviewIT extends LearningProgressIT {
  @MockitoSpyBean private Clock reviewClock;
  @SuppressWarnings("unchecked")
  private <T> T field(String name) { return (T) ReflectionTestUtils.getField(this,name); }

  @Test void reviewRealChangeAdvancesVersionWhenClockDoesNotAdvance() {
    Object session = ReflectionTestUtils.invokeMethod(this,"login","usera","Password123!");
    UUID enrollmentId = ReflectionTestUtils.invokeMethod(this,"createEnrollmentForUser",session);
    EnrollmentRepository enrollments = field("enrollmentRepository");
    var before = enrollments.findById(enrollmentId).orElseThrow();
    Instant unchanged = before.getUpdatedAt();
    doReturn(unchanged).when(reviewClock).instant();
    ActivityRepository activities = field("activityRepository");
    UUID revisionId = field("revisionId");
    var activity = activities.findByLessonWeekPhaseRevisionId(revisionId).stream()
        .filter(a -> a.getActivityType()==ActivityType.READING).findFirst().orElseThrow();
    HttpHeaders headers = ReflectionTestUtils.invokeMethod(this,"authHeaders",session);
    String baseUrl = ReflectionTestUtils.invokeMethod(this,"baseUrl");
    TestRestTemplate http = field("restTemplate");
    var response = http.exchange(baseUrl+"/enrollments/"+enrollmentId+"/activities/"+activity.getId()+"/progress",
        HttpMethod.PUT,new HttpEntity<>(new UpdateProgressRequest(ActivityStatus.COMPLETED,before.getVersion()),headers),ProgressResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().enrollmentVersion()).as("meaningful progress change must advance enrollment version even at same instant").isEqualTo(before.getVersion()+1);
    assertThat(enrollments.findById(enrollmentId).orElseThrow().getVersion()).isEqualTo(before.getVersion()+1);
  }
}
