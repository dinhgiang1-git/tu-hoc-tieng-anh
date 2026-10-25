package vn.englishlearning.curriculum.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.curriculum.api.CatalogResponses.*;
import vn.englishlearning.curriculum.application.CurriculumCatalogQueryService;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class CurriculumCatalogControllerTest {

  private CurriculumCatalogQueryService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(CurriculumCatalogQueryService.class);
    CurriculumCatalogController controller = new CurriculumCatalogController(service);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/curricula returns 200 and list of published curricula")
  void list_returnsOkWithSummaries() throws Exception {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    CurriculumSummaryResponse summary =
        new CurriculumSummaryResponse(
            curriculumId,
            "pilot-curriculum",
            "Lộ trình Thử nghiệm",
            "Mô tả",
            "en-GB",
            revisionId,
            "1.0.0");
    when(service.list()).thenReturn(List.of(summary));

    mockMvc
        .perform(get("/api/v1/curricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(curriculumId.toString()))
        .andExpect(jsonPath("$[0].slug").value("pilot-curriculum"))
        .andExpect(jsonPath("$[0].title").value("Lộ trình Thử nghiệm"))
        .andExpect(jsonPath("$[0].targetLocale").value("en-GB"))
        .andExpect(jsonPath("$[0].currentRevisionId").value(revisionId.toString()))
        .andExpect(jsonPath("$[0].currentRevisionVersion").value("1.0.0"));
  }

  @Test
  @DisplayName("GET /api/v1/curricula/{slug} returns 200 with full curriculum detail")
  void detail_returnsOkWithCurriculumDetail() throws Exception {
    UUID curriculumId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    CurriculumDetailResponse detail =
        new CurriculumDetailResponse(
            curriculumId,
            "pilot-curriculum",
            "Lộ trình Thử nghiệm",
            "Mô tả chi tiết",
            "en-GB",
            List.of("Curriculum Lead"),
            new ProvenanceResponse(
                "Source Author", "https://example.com", "CC-BY-4.0", true, "UNVERIFIED_DRAFT"),
            new RevisionOutlineResponse(
                revisionId,
                "1.0.0",
                "PUBLISHED",
                List.of(
                    new PhaseOutlineDto(
                        UUID.randomUUID(),
                        "phase.01",
                        "Giai đoạn 1",
                        1,
                        List.of(
                            new WeekOutlineDto(
                                UUID.randomUUID(),
                                "week.01",
                                "Tuần 1",
                                1,
                                1,
                                List.of(
                                    new LessonOutlineDto(
                                        UUID.randomUUID(),
                                        "lesson.01",
                                        "Bài 1",
                                        1,
                                        25,
                                        true))))))));
    when(service.detail("pilot-curriculum")).thenReturn(detail);

    mockMvc
        .perform(get("/api/v1/curricula/pilot-curriculum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(curriculumId.toString()))
        .andExpect(jsonPath("$.slug").value("pilot-curriculum"))
        .andExpect(jsonPath("$.targetLocale").value("en-GB"))
        .andExpect(jsonPath("$.authors[0]").value("Curriculum Lead"))
        .andExpect(jsonPath("$.provenance.sourceName").value("Source Author"))
        .andExpect(jsonPath("$.provenance.verificationStatus").value("UNVERIFIED_DRAFT"))
        .andExpect(jsonPath("$.revision.id").value(revisionId.toString()))
        .andExpect(jsonPath("$.revision.phases[0].weeks[0].lessons[0].title").value("Bài 1"))
        .andExpect(jsonPath("$.revision.phases[0].weeks[0].lessons[0].estimatedMinutes").value(25))
        .andExpect(jsonPath("$.revision.phases[0].weeks[0].lessons[0].required").value(true));
  }

  @Test
  @DisplayName("GET /api/v1/curricula/{slug} returns 404 Problem Details when slug not found")
  void detail_whenNotFound_returns404ProblemDetails() throws Exception {
    when(service.detail("missing-curriculum"))
        .thenThrow(new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/curricula/missing-curriculum"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("GET /api/v1/revisions/{revisionId}/outline returns 200 with outline structure")
  void outline_returnsOkWithOutline() throws Exception {
    UUID revisionId = UUID.randomUUID();
    RevisionOutlineResponse outline =
        new RevisionOutlineResponse(
            revisionId,
            "1.0.0",
            "PUBLISHED",
            List.of(
                new PhaseOutlineDto(
                    UUID.randomUUID(),
                    "phase.01",
                    "Giai đoạn 1",
                    1,
                    List.of(
                        new WeekOutlineDto(
                            UUID.randomUUID(),
                            "week.01",
                            "Tuần 1",
                            1,
                            1,
                            List.of(
                                new LessonOutlineDto(
                                    UUID.randomUUID(), "lesson.01", "Bài 1", 1, 25, true)))))));
    when(service.outline(revisionId)).thenReturn(outline);

    mockMvc
        .perform(get("/api/v1/revisions/" + revisionId + "/outline"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(revisionId.toString()))
        .andExpect(jsonPath("$.version").value("1.0.0"))
        .andExpect(jsonPath("$.status").value("PUBLISHED"))
        .andExpect(jsonPath("$.phases[0].key").value("phase.01"))
        .andExpect(jsonPath("$.phases[0].weeks[0].lessons[0].title").value("Bài 1"));
  }

  @Test
  @DisplayName(
      "GET /api/v1/revisions/{revisionId}/outline returns 404 Problem Details when revisionId not found")
  void outline_whenNotFound_returns404ProblemDetails() throws Exception {
    UUID unknownRevisionId = UUID.randomUUID();
    when(service.outline(unknownRevisionId))
        .thenThrow(new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/revisions/" + unknownRevisionId + "/outline"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }
}
