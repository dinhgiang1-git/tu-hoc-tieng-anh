package vn.englishlearning.scheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

/**
 * Pure domain service for calculating lesson schedules. Independent of repositories, security
 * context, and system clocks.
 */
public final class SchedulePlanner {

  private static final int MAX_PLANNING_HORIZON_YEARS = 50;

  private SchedulePlanner() {}

  /** Plans the schedule for a list of lessons given a start date and selected study days. */
  public static SchedulePlan plan(
      List<SchedulableLesson> lessons, LocalDate startDate, Collection<DayOfWeek> studyDays) {
    return plan(lessons, startDate, studyDays, Set.of());
  }

  /** Plans the schedule with optional exclusions (e.g., holidays/rest days). */
  public static SchedulePlan plan(
      List<SchedulableLesson> lessons,
      LocalDate startDate,
      Collection<DayOfWeek> studyDays,
      Set<LocalDate> exclusions) {
    if (startDate == null) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Ngày bắt đầu không được để trống.");
    }
    if (studyDays == null || studyDays.isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Phải chọn ít nhất một ngày học trong tuần.");
    }
    for (DayOfWeek day : studyDays) {
      if (day == null) {
        throw new DomainException(
            ErrorCode.VALIDATION_FAILED, "Ngày học trong tuần không được chứa giá trị rỗng.");
      }
    }

    Set<DayOfWeek> validDays = EnumSet.copyOf(studyDays);
    Set<LocalDate> exclusionSet = exclusions != null ? Set.copyOf(exclusions) : Set.of();

    if (lessons == null || lessons.isEmpty()) {
      return new SchedulePlan(List.of(), null);
    }

    List<SchedulableLesson> requiredLessons =
        lessons.stream().filter(SchedulableLesson::required).toList();

    if (requiredLessons.isEmpty()) {
      return new SchedulePlan(List.of(), null);
    }

    LocalDate maxAllowedDate = startDate.plusYears(MAX_PLANNING_HORIZON_YEARS);
    LocalDate current = startDate;
    List<PlannedLesson> plannedLessons = new ArrayList<>(requiredLessons.size());

    for (SchedulableLesson lesson : requiredLessons) {
      while (!validDays.contains(current.getDayOfWeek()) || exclusionSet.contains(current)) {
        current = current.plusDays(1);
        if (current.isAfter(maxAllowedDate)) {
          throw new DomainException(
              ErrorCode.VALIDATION_FAILED, "Khoảng ngày phân bổ vượt quá giới hạn tối đa.");
        }
      }
      plannedLessons.add(new PlannedLesson(lesson.lessonId(), current));
      current = current.plusDays(1);
    }

    LocalDate estimatedEndDate = plannedLessons.get(plannedLessons.size() - 1).plannedDate();
    return new SchedulePlan(plannedLessons, estimatedEndDate);
  }
}
