package vn.englishlearning.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.curriculum.api.CatalogResponses.*;
import vn.englishlearning.curriculum.infrastructure.CurriculumCatalogRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumCatalogRepository.*;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

class CurriculumCatalogQueryServiceTest {

  private CurriculumCatalogRepository repository;
  private ObjectMapper objectMapper;
  private CurriculumCatalogQueryService service;

  @BeforeEach
  void setUp() {
    repository = mock(CurriculumCatalogRepository.class);
    objectMapper = new ObjectMapper();
    service = new CurriculumCatalogQueryService(repository, objectMapper);
  }

  @Test
  @DisplayName("list() delegates to repository.listPublished() and returns published curricula")
  void list_returnsPublishedCurricula() {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    CurriculumSummaryResponse summary =
        new CurriculumSummaryResponse(
            curriculumId,
            "pilot-curriculum",
            "Lộ trình Thử nghiệm",
            "Mô tả thử nghiệm",
            "en-GB",
            revisionId,
            "1.0.0");
    when(repository.listPublished()).thenReturn(List.of(summary));

    List<CurriculumSummaryResponse> result = service.list();

    assertThat(result).containsExactly(summary);
    verify(repository).listPublished();
  }

  @Test
  @DisplayName(
      "detail(slug) returns full curriculum detail with deserialized authors, provenance, and outline")
  void detail_whenFound_returnsDetailWithOutline() {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    UUID phaseId = UUID.randomUUID();
    UUID weekId = UUID.randomUUID();
    UUID lessonId = UUID.randomUUID();

    RevisionMetadata metadata =
        new RevisionMetadata(
            curriculumId,
            "pilot-curriculum",
            "en-GB",
            revisionId,
            "1.0.0",
            "PUBLISHED",
            "Lộ trình Thử nghiệm",
            "Mô tả thử nghiệm",
            "[\"Author One\", \"Author Two\"]",
            """
            {
              "sourceName": "Pilot Source",
              "sourceUrl": "https://example.com/source",
              "sourceLicense": "CC-BY-4.0",
              "authorCompiled": true,
              "verificationStatus": "UNVERIFIED_DRAFT"
            }
            """);

    when(repository.findPublished("pilot-curriculum")).thenReturn(Optional.of(metadata));
    when(repository.phases(revisionId))
        .thenReturn(List.of(new PhaseRow(phaseId, "phase.01", "Giai đoạn 1", 1)));
    when(repository.weeks(revisionId))
        .thenReturn(List.of(new WeekRow(phaseId, weekId, "week.01", "Tuần 1", 1, 1)));
    when(repository.lessons(revisionId))
        .thenReturn(
            List.of(
                new LessonRow(
                    weekId, new LessonOutlineDto(lessonId, "lesson.01", "Bài 1", 1, 25, true))));

    CurriculumDetailResponse detail = service.detail("pilot-curriculum");

    assertThat(detail.id()).isEqualTo(curriculumId);
    assertThat(detail.slug()).isEqualTo("pilot-curriculum");
    assertThat(detail.targetLocale()).isEqualTo("en-GB");
    assertThat(detail.authors()).containsExactly("Author One", "Author Two");
    assertThat(detail.provenance().sourceName()).isEqualTo("Pilot Source");
    assertThat(detail.provenance().sourceUrl()).isEqualTo("https://example.com/source");
    assertThat(detail.provenance().verificationStatus()).isEqualTo("UNVERIFIED_DRAFT");
    assertThat(detail.provenance().authorCompiled()).isTrue();

    RevisionOutlineResponse outline = detail.revision();
    assertThat(outline.id()).isEqualTo(revisionId);
    assertThat(outline.version()).isEqualTo("1.0.0");
    assertThat(outline.status()).isEqualTo("PUBLISHED");
    assertThat(outline.phases()).hasSize(1);
    assertThat(outline.phases().get(0).weeks()).hasSize(1);
    assertThat(outline.phases().get(0).weeks().get(0).lessons()).hasSize(1);
    assertThat(outline.phases().get(0).weeks().get(0).lessons().get(0).title()).isEqualTo("Bài 1");
  }

  @Test
  @DisplayName(
      "detail(slug) throws DomainException(RESOURCE_NOT_FOUND) when slug is not found or unpublished")
  void detail_whenNotFound_throwsNotFoundDomainException() {
    when(repository.findPublished("unknown-slug")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detail("unknown-slug"))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  @DisplayName("outline(revisionId) returns structured outline with stable ordering")
  void outline_whenFound_returnsStructuredOutline() {
    UUID revisionId = UUID.randomUUID();
    UUID phaseId = UUID.randomUUID();
    UUID weekId1 = UUID.randomUUID();
    UUID weekId2 = UUID.randomUUID();
    UUID lessonId1 = UUID.randomUUID();
    UUID lessonId2 = UUID.randomUUID();

    RevisionMetadata metadata =
        new RevisionMetadata(
            UUID.randomUUID(),
            "pilot-curriculum",
            "en-GB",
            revisionId,
            "1.0.0",
            "PUBLISHED",
            "Lộ trình Thử nghiệm",
            "Mô tả",
            "[]",
            "{}");

    when(repository.findVisibleRevision(revisionId)).thenReturn(Optional.of(metadata));
    when(repository.phases(revisionId))
        .thenReturn(List.of(new PhaseRow(phaseId, "phase.01", "Phase 1", 1)));
    when(repository.weeks(revisionId))
        .thenReturn(
            List.of(
                new WeekRow(phaseId, weekId1, "week.01", "Week 1", 1, 1),
                new WeekRow(phaseId, weekId2, "week.02", "Week 2", 2, 2)));
    when(repository.lessons(revisionId))
        .thenReturn(
            List.of(
                new LessonRow(
                    weekId1, new LessonOutlineDto(lessonId1, "lesson.01", "Lesson 1", 1, 20, true)),
                new LessonRow(
                    weekId1,
                    new LessonOutlineDto(lessonId2, "lesson.02", "Lesson 2", 2, 30, false))));

    RevisionOutlineResponse outline = service.outline(revisionId);

    assertThat(outline.id()).isEqualTo(revisionId);
    assertThat(outline.phases()).hasSize(1);
    PhaseOutlineDto phase = outline.phases().get(0);
    assertThat(phase.weeks()).hasSize(2);
    assertThat(phase.weeks().get(0).lessons()).hasSize(2);
    assertThat(phase.weeks().get(0).lessons().get(0).orderIndex()).isEqualTo(1);
    assertThat(phase.weeks().get(0).lessons().get(1).orderIndex()).isEqualTo(2);
    assertThat(phase.weeks().get(1).lessons()).isEmpty();
  }

  @Test
  @DisplayName(
      "outline(revisionId) throws DomainException(RESOURCE_NOT_FOUND) when revision is not visible/published")
  void outline_whenNotFound_throwsNotFoundDomainException() {
    UUID unknownId = UUID.randomUUID();
    when(repository.findVisibleRevision(unknownId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.outline(unknownId))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
  }
}
