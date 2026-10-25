package vn.englishlearning.learning.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.enrollment.domain.*;
import vn.englishlearning.enrollment.infrastructure.*;
import vn.englishlearning.learning.domain.*;
import vn.englishlearning.learning.infrastructure.ActivityProgressRepository;

class G301ReviewTest extends TodayQueryServiceTest {
  @SuppressWarnings("unchecked")
  private <T> T field(String name) { return (T) ReflectionTestUtils.getField(this, name); }
  private TodayQueryService service() {
    return ReflectionTestUtils.invokeMethod(this, "createServiceWithTime", "2026-10-28T03:00:00Z");
  }
  private Enrollment enrollment(EnrollmentStatus status) {
    Enrollment enrollment = ReflectionTestUtils.invokeMethod(this, "createEnrollment", status);
    EnrollmentRepository repository = field("enrollmentRepository");
    when(repository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
    return enrollment;
  }
  @Test void reviewOptionalActivityDoesNotKeepLessonOpen() {
    Enrollment e = enrollment(EnrollmentStatus.ACTIVE);
    Activity optional = field("act1_2");
    ReflectionTestUtils.setField(optional, "isRequired", false);
    Activity required = field("act1_1");
    ActivityProgressRepository progress = field("activityProgressRepository");
    Instant now = Instant.parse("2026-10-28T03:00:00Z");
    when(progress.findByEnrollmentId(e.getId())).thenReturn(List.of(new ActivityProgress(e.getId(), required.getId(), ActivityStatus.COMPLETED, now)));
    Lesson first = field("lesson1"); Lesson second = field("lesson2");
    LessonScheduleRepository schedules = field("lessonScheduleRepository");
    when(schedules.findByEnrollmentIdOrderByPlannedDateAsc(e.getId())).thenReturn(List.of(
      new LessonSchedule(e.getId(), first.getId(), LocalDate.of(2026,10,26), LocalDate.of(2026,10,26), now),
      new LessonSchedule(e.getId(), second.getId(), LocalDate.of(2026,10,28), LocalDate.of(2026,10,28), now)));
    var result = service().getToday(e.getId());
    assertThat(result.state()).as("optional activity must not keep a completed required lesson in progress").isEqualTo("DUE_TODAY");
    assertThat(result.primaryAction().lessonId()).isEqualTo(second.getId());
  }
  @Test void reviewPausedMustTakePriorityOverCompletedCounts() {
    Enrollment e = enrollment(EnrollmentStatus.PAUSED);
    var completed = List.of("act1_1", "act1_2", "act2_1", "act2_2", "act3_1").stream().map(name -> {
      Activity a = field(name);
      return new ActivityProgress(e.getId(), a.getId(), ActivityStatus.COMPLETED, Instant.parse("2026-10-28T03:00:00Z"));
    }).toList();
    ActivityProgressRepository progress = field("activityProgressRepository");
    when(progress.findByEnrollmentId(e.getId())).thenReturn(completed);
    assertThat(service().getToday(e.getId()).state()).isEqualTo("PAUSED");
  }
  @Test void reviewTodayDoesNotFetchActivitiesOncePerLesson() {
    Enrollment e = enrollment(EnrollmentStatus.ACTIVE);
    service().getToday(e.getId());
    ActivityRepository activities = field("activityRepository");
    verify(activities, never()).findByLessonIdOrderByOrderIndexAsc(any(UUID.class));
  }
}
