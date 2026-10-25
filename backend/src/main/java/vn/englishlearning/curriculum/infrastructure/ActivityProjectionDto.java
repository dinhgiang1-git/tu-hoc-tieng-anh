package vn.englishlearning.curriculum.infrastructure;

import java.util.UUID;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;

public record ActivityProjectionDto(
    UUID id,
    UUID lessonId,
    int orderIndex,
    String title,
    ActivityType type,
    boolean isRequired,
    CompletionPolicy completionPolicy) {}
