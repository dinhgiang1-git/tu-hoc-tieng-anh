package vn.englishlearning.contentops.domain;

import java.util.UUID;

public record ImportSummary(
    UUID revisionId,
    String curriculumKey,
    String version,
    String status,
    String checksum,
    int phaseCount,
    int weekCount,
    int lessonCount,
    int activityCount,
    int resourceCount,
    boolean newlyImported) {}
