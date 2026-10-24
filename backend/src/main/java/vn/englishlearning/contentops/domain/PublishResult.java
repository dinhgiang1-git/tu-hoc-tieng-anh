package vn.englishlearning.contentops.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record PublishResult(
    UUID revisionId,
    String curriculumKey,
    String version,
    String status,
    Instant publishedAt,
    List<String> warnings) {

  public PublishResult {
    warnings = warnings == null ? List.of() : Collections.unmodifiableList(warnings);
  }
}
