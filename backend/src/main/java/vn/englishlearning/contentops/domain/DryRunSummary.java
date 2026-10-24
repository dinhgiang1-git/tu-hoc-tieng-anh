package vn.englishlearning.contentops.domain;

import java.util.Collections;
import java.util.List;

public record DryRunSummary(
    String curriculumKey,
    String version,
    String title,
    int phaseCount,
    int weekCount,
    int lessonCount,
    int requiredLessonCount,
    int optionalLessonCount,
    int activityCount,
    int effectivelyRequiredActivityCount,
    int resourceCount,
    int unverifiedResourceCount,
    String checksum,
    List<ValidationWarning> warnings) {

  public DryRunSummary {
    warnings = warnings == null ? List.of() : Collections.unmodifiableList(warnings);
  }
}
