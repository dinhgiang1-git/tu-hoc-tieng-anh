package vn.englishlearning.contentops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.contentops.domain.PublishResult;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.domain.Resource;
import vn.englishlearning.curriculum.domain.RevisionStatus;
import vn.englishlearning.curriculum.domain.VerificationStatus;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumPhaseRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.ResourceRepository;

@Service
public class PublishCurriculumRevisionService {

  private static final Logger log = LoggerFactory.getLogger(PublishCurriculumRevisionService.class);

  private final CurriculumRevisionRepository revisionRepository;
  private final CurriculumPhaseRepository phaseRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ResourceRepository resourceRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public PublishCurriculumRevisionService(
      CurriculumRevisionRepository revisionRepository,
      CurriculumPhaseRepository phaseRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ResourceRepository resourceRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.revisionRepository = revisionRepository;
    this.phaseRepository = phaseRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.resourceRepository = resourceRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public PublishResult publish(String curriculumKey, String version) {
    CurriculumRevision revision =
        revisionRepository
            .findByCurriculumCurriculumKeyAndVersion(curriculumKey, version)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Curriculum revision not found for " + curriculumKey + " v" + version));

    return publishInternal(revision);
  }

  @Transactional
  public PublishResult publishById(UUID revisionId) {
    CurriculumRevision revision =
        revisionRepository
            .findById(revisionId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Curriculum revision not found with id: " + revisionId));

    return publishInternal(revision);
  }

  private PublishResult publishInternal(CurriculumRevision revision) {
    if (revision.getStatus() == RevisionStatus.PUBLISHED) {
      throw new IllegalStateException(
          "Curriculum revision "
              + revision.getCurriculum().getCurriculumKey()
              + " v"
              + revision.getVersion()
              + " is already PUBLISHED");
    }

    if (revision.getStatus() == RevisionStatus.ARCHIVED) {
      throw new IllegalStateException("Cannot publish curriculum revision from ARCHIVED status");
    }

    // Validate completeness
    var phases = phaseRepository.findByRevisionIdOrderByOrderIndexAsc(revision.getId());
    if (phases.isEmpty()) {
      throw new IllegalStateException("Cannot publish curriculum revision without phases");
    }

    List<Lesson> lessons = lessonRepository.findByWeekPhaseRevisionId(revision.getId());
    if (lessons.isEmpty()) {
      throw new IllegalStateException("Cannot publish curriculum revision without lessons");
    }

    for (Lesson lesson : lessons) {
      if (lesson.isRequired()) {
        if (lesson.getContentMarkdown() == null || lesson.getContentMarkdown().trim().isEmpty()) {
          throw new IllegalStateException(
              "Required lesson '" + lesson.getLessonKey() + "' has empty markdown content");
        }
        List<Activity> acts = activityRepository.findByLessonIdOrderByOrderIndexAsc(lesson.getId());
        if (acts.isEmpty()) {
          throw new IllegalStateException(
              "Required lesson '" + lesson.getLessonKey() + "' has no activities");
        }
      }
    }

    Instant now = Instant.now(clock);
    revision.publish(now);
    CurriculumRevision saved = revisionRepository.save(revision);

    List<String> warnings = new ArrayList<>();

    // Provenance verification check
    try {
      JsonNode prov = objectMapper.readTree(saved.getProvenance());
      String provStatus = prov.path("verificationStatus").asText();
      if ("UNVERIFIED_DRAFT".equals(provStatus)) {
        warnings.add(
            "Provenance status is UNVERIFIED_DRAFT: Content is pedagogical pilot draft and not a fully verified replica of the source Google Sheet.");
      }
    } catch (Exception ignored) {
      // Ignored if provenance is not standard JSON
    }

    // Resource verification status check
    List<Resource> resources = resourceRepository.findByRevisionId(saved.getId());
    long unverifiedCount =
        resources.stream()
            .filter(r -> r.getVerificationStatus() == VerificationStatus.UNVERIFIED_DRAFT)
            .count();
    if (unverifiedCount > 0) {
      warnings.add(
          unverifiedCount + " external resource(s) in this revision remain UNVERIFIED_DRAFT.");
    }

    log.info(
        "Successfully published curriculum revision {} v{} (revisionId: {})",
        saved.getCurriculum().getCurriculumKey(),
        saved.getVersion(),
        saved.getId());

    return new PublishResult(
        saved.getId(),
        saved.getCurriculum().getCurriculumKey(),
        saved.getVersion(),
        saved.getStatus().name(),
        saved.getPublishedAt(),
        warnings);
  }
}
