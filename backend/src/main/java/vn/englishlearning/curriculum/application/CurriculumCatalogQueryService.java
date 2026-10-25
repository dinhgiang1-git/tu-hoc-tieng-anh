package vn.englishlearning.curriculum.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.curriculum.api.CatalogResponses.*;
import vn.englishlearning.curriculum.infrastructure.CurriculumCatalogRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumCatalogRepository.*;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

@Service
@Transactional(readOnly = true)
public class CurriculumCatalogQueryService {
  private final CurriculumCatalogRepository repository;
  private final ObjectMapper mapper;

  public CurriculumCatalogQueryService(
      CurriculumCatalogRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public List<CurriculumSummaryResponse> list() {
    return repository.listPublished();
  }

  public CurriculumDetailResponse detail(String slug) {
    RevisionMetadata revision =
        repository.findPublished(slug).orElseThrow(CurriculumCatalogQueryService::notFound);
    try {
      return new CurriculumDetailResponse(
          revision.curriculumId(),
          revision.slug(),
          revision.title(),
          revision.description(),
          revision.locale(),
          mapper.readValue(revision.authors(), new TypeReference<List<String>>() {}),
          mapper.readValue(revision.provenance(), ProvenanceResponse.class),
          outline(revision));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Stored curriculum attribution is invalid", ex);
    }
  }

  public RevisionOutlineResponse outline(UUID id) {
    return outline(
        repository.findVisibleRevision(id).orElseThrow(CurriculumCatalogQueryService::notFound));
  }

  private RevisionOutlineResponse outline(RevisionMetadata revision) {
    var phases = repository.phases(revision.revisionId());
    var weeksByPhase =
        repository.weeks(revision.revisionId()).stream()
            .collect(
                Collectors.groupingBy(
                    WeekRow::phaseId, java.util.LinkedHashMap::new, Collectors.toList()));
    var lessonsByWeek =
        repository.lessons(revision.revisionId()).stream()
            .collect(
                Collectors.groupingBy(
                    LessonRow::weekId, java.util.LinkedHashMap::new, Collectors.toList()));
    return new RevisionOutlineResponse(
        revision.revisionId(),
        revision.version(),
        revision.status(),
        phases.stream()
            .map(
                phase ->
                    new PhaseOutlineDto(
                        phase.id(),
                        phase.key(),
                        phase.title(),
                        phase.orderIndex(),
                        weeksByPhase.getOrDefault(phase.id(), List.of()).stream()
                            .map(
                                week ->
                                    new WeekOutlineDto(
                                        week.id(),
                                        week.key(),
                                        week.title(),
                                        week.weekNumber(),
                                        week.orderIndex(),
                                        lessonsByWeek.getOrDefault(week.id(), List.of()).stream()
                                            .map(LessonRow::lesson)
                                            .toList()))
                            .toList()))
            .toList());
  }

  private static DomainException notFound() {
    return new DomainException(ErrorCode.RESOURCE_NOT_FOUND);
  }
}
