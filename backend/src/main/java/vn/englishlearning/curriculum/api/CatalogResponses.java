package vn.englishlearning.curriculum.api;

import java.util.List;
import java.util.UUID;

public final class CatalogResponses {
  private CatalogResponses() {}

  public record CurriculumSummaryResponse(
      UUID id,
      String slug,
      String title,
      String description,
      String targetLocale,
      UUID currentRevisionId,
      String currentRevisionVersion) {}

  public record CurriculumDetailResponse(
      UUID id,
      String slug,
      String title,
      String description,
      String targetLocale,
      List<String> authors,
      ProvenanceResponse provenance,
      RevisionOutlineResponse revision) {}

  public record ProvenanceResponse(
      String sourceName,
      String sourceUrl,
      String sourceLicense,
      boolean authorCompiled,
      String verificationStatus) {}

  public record RevisionOutlineResponse(
      UUID id, String version, String status, List<PhaseOutlineDto> phases) {}

  public record PhaseOutlineDto(
      UUID id, String key, String title, int orderIndex, List<WeekOutlineDto> weeks) {}

  public record WeekOutlineDto(
      UUID id,
      String key,
      String title,
      int weekNumber,
      int orderIndex,
      List<LessonOutlineDto> lessons) {}

  public record LessonOutlineDto(
      UUID id, String key, String title, int orderIndex, int estimatedMinutes, boolean required) {}
}
