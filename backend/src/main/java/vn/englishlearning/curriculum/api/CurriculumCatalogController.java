package vn.englishlearning.curriculum.api;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.curriculum.api.CatalogResponses.*;
import vn.englishlearning.curriculum.application.CurriculumCatalogQueryService;

@RestController
@RequestMapping("/api/v1")
public class CurriculumCatalogController {
  private final CurriculumCatalogQueryService service;

  public CurriculumCatalogController(CurriculumCatalogQueryService service) {
    this.service = service;
  }

  @GetMapping("/curricula")
  public List<CurriculumSummaryResponse> list() {
    return service.list();
  }

  @GetMapping("/curricula/{slug}")
  public CurriculumDetailResponse detail(@PathVariable String slug) {
    return service.detail(slug);
  }

  @GetMapping("/revisions/{revisionId}/outline")
  public RevisionOutlineResponse outline(@PathVariable UUID revisionId) {
    return service.outline(revisionId);
  }
}
