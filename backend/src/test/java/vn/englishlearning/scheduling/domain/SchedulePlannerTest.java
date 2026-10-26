package vn.englishlearning.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

class SchedulePlannerTest {

  private final UUID l1 = UUID.randomUUID();
  private final UUID l2 = UUID.randomUUID();
  private final UUID l3 = UUID.randomUUID();
  private final UUID l4 = UUID.randomUUID();
  private final UUID l5 = UUID.randomUUID();

  @Test
  @DisplayName(
      "Mon/Wed/Fri study days: lessons are placed on valid study days starting from Monday")
  void plan_monWedFri_placedCorrectly() {
    LocalDate startDate = LocalDate.of(2026, 10, 26); // Monday
    List<SchedulableLesson> lessons =
        List.of(
            new SchedulableLesson(l1, true),
            new SchedulableLesson(l2, true),
            new SchedulableLesson(l3, true),
            new SchedulableLesson(l4, true));

    SchedulePlan plan =
        SchedulePlanner.plan(
            lessons, startDate, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

    assertThat(plan.plannedLessons()).hasSize(4);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 26)); // Mon
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(plan.plannedLessons().get(2).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 30)); // Fri
    assertThat(plan.plannedLessons().get(3).plannedDate())
        .isEqualTo(LocalDate.of(2026, 11, 2)); // Mon next week
    assertThat(plan.estimatedEndDate()).isEqualTo(LocalDate.of(2026, 11, 2));
  }

  @Test
  @DisplayName("Single day per week: lessons are placed weekly on that day")
  void plan_singleDayPerWeek_placedWeekly() {
    LocalDate startDate = LocalDate.of(2026, 10, 26); // Monday
    List<SchedulableLesson> lessons =
        List.of(
            new SchedulableLesson(l1, true),
            new SchedulableLesson(l2, true),
            new SchedulableLesson(l3, true));

    SchedulePlan plan = SchedulePlanner.plan(lessons, startDate, List.of(DayOfWeek.SUNDAY));

    assertThat(plan.plannedLessons()).hasSize(3);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2026, 11, 1)); // Sunday
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2026, 11, 8)); // Next Sunday
    assertThat(plan.plannedLessons().get(2).plannedDate())
        .isEqualTo(LocalDate.of(2026, 11, 15)); // Next Sunday
    assertThat(plan.estimatedEndDate()).isEqualTo(LocalDate.of(2026, 11, 15));
  }

  @Test
  @DisplayName("Start date on rest day: advances to the first valid study day")
  void plan_startDateOnRestDay_advancesToNextStudyDay() {
    LocalDate startDate = LocalDate.of(2026, 10, 27); // Tuesday (rest day for MWF)
    List<SchedulableLesson> lessons =
        List.of(new SchedulableLesson(l1, true), new SchedulableLesson(l2, true));

    SchedulePlan plan =
        SchedulePlanner.plan(
            lessons, startDate, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

    assertThat(plan.plannedLessons()).hasSize(2);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 30)); // Fri
    assertThat(plan.estimatedEndDate()).isEqualTo(LocalDate.of(2026, 10, 30));
  }

  @Test
  @DisplayName("Crosses month, year, and leap year accurately")
  void plan_crossesLeapYear_correctDates() {
    // 2024 is a leap year; Feb 28 2024 is Wednesday, Feb 29 is Thursday, March 1 is Friday
    LocalDate startDate = LocalDate.of(2024, 2, 28); // Wednesday
    List<SchedulableLesson> lessons =
        List.of(
            new SchedulableLesson(l1, true),
            new SchedulableLesson(l2, true),
            new SchedulableLesson(l3, true));

    SchedulePlan plan =
        SchedulePlanner.plan(lessons, startDate, List.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));

    assertThat(plan.plannedLessons()).hasSize(3);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2024, 2, 29)); // Thu (Leap Day)
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2024, 3, 1)); // Fri
    assertThat(plan.plannedLessons().get(2).plannedDate())
        .isEqualTo(LocalDate.of(2024, 3, 7)); // Next Thu
    assertThat(plan.estimatedEndDate()).isEqualTo(LocalDate.of(2024, 3, 7));
  }

  @Test
  @DisplayName("Optional lessons do not occupy schedule slots")
  void plan_optionalLessonsSkipped() {
    LocalDate startDate = LocalDate.of(2026, 10, 26); // Monday
    List<SchedulableLesson> lessons =
        List.of(
            new SchedulableLesson(l1, true),
            new SchedulableLesson(l2, false), // optional -> skipped
            new SchedulableLesson(l3, true),
            new SchedulableLesson(l4, false)); // optional -> skipped

    SchedulePlan plan =
        SchedulePlanner.plan(
            lessons, startDate, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY));

    assertThat(plan.plannedLessons()).hasSize(2);
    assertThat(plan.plannedLessons().get(0).lessonId()).isEqualTo(l1);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 26)); // Mon
    assertThat(plan.plannedLessons().get(1).lessonId()).isEqualTo(l3);
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 28)); // Wed
    assertThat(plan.estimatedEndDate()).isEqualTo(LocalDate.of(2026, 10, 28));
  }

  @Test
  @DisplayName("Pedagogical order from input is strictly preserved")
  void plan_preservesInputOrder() {
    LocalDate startDate = LocalDate.of(2026, 10, 26);
    List<SchedulableLesson> lessons =
        List.of(
            new SchedulableLesson(l5, true),
            new SchedulableLesson(l2, true),
            new SchedulableLesson(l1, true));

    SchedulePlan plan =
        SchedulePlanner.plan(lessons, startDate, List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));

    assertThat(plan.plannedLessons())
        .extracting(PlannedLesson::lessonId)
        .containsExactly(l5, l2, l1);
  }

  @Test
  @DisplayName("Empty or all-optional lessons list returns empty plan and null estimatedEndDate")
  void plan_emptyOrAllOptional_returnsEmptyPlan() {
    LocalDate startDate = LocalDate.of(2026, 10, 26);

    SchedulePlan emptyListPlan =
        SchedulePlanner.plan(List.of(), startDate, List.of(DayOfWeek.MONDAY));
    assertThat(emptyListPlan.plannedLessons()).isEmpty();
    assertThat(emptyListPlan.estimatedEndDate()).isNull();

    SchedulePlan allOptionalPlan =
        SchedulePlanner.plan(
            List.of(new SchedulableLesson(l1, false)), startDate, List.of(DayOfWeek.MONDAY));
    assertThat(allOptionalPlan.plannedLessons()).isEmpty();
    assertThat(allOptionalPlan.estimatedEndDate()).isNull();
  }

  @Test
  @DisplayName("Null or empty studyDays throws DomainException VALIDATION_FAILED")
  void plan_invalidStudyDays_throwsValidationFailed() {
    LocalDate startDate = LocalDate.of(2026, 10, 26);
    List<SchedulableLesson> lessons = List.of(new SchedulableLesson(l1, true));

    assertThatThrownBy(() -> SchedulePlanner.plan(lessons, startDate, null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

    assertThatThrownBy(() -> SchedulePlanner.plan(lessons, startDate, List.of()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);

    List<DayOfWeek> listWithNull = new ArrayList<>();
    listWithNull.add(DayOfWeek.MONDAY);
    listWithNull.add(null);
    assertThatThrownBy(() -> SchedulePlanner.plan(lessons, startDate, listWithNull))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("Null startDate throws DomainException VALIDATION_FAILED")
  void plan_nullStartDate_throwsValidationFailed() {
    List<SchedulableLesson> lessons = List.of(new SchedulableLesson(l1, true));

    assertThatThrownBy(() -> SchedulePlanner.plan(lessons, null, List.of(DayOfWeek.MONDAY)))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
  }

  @Test
  @DisplayName("Exclusions are respected and skipped")
  void plan_withExclusions_skipsExcludedDates() {
    LocalDate startDate = LocalDate.of(2026, 10, 26); // Monday
    LocalDate excludedWed = LocalDate.of(2026, 10, 28);
    List<SchedulableLesson> lessons =
        List.of(new SchedulableLesson(l1, true), new SchedulableLesson(l2, true));

    SchedulePlan plan =
        SchedulePlanner.plan(
            lessons,
            startDate,
            List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            Set.of(excludedWed));

    assertThat(plan.plannedLessons()).hasSize(2);
    assertThat(plan.plannedLessons().get(0).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 26)); // Mon
    assertThat(plan.plannedLessons().get(1).plannedDate())
        .isEqualTo(LocalDate.of(2026, 10, 30)); // Fri (Wed skipped!)
  }
}
