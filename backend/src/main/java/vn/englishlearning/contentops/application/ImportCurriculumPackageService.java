package vn.englishlearning.contentops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.assessment.domain.QuizOption;
import vn.englishlearning.assessment.domain.QuizQuestion;
import vn.englishlearning.assessment.infrastructure.QuizQuestionRepository;
import vn.englishlearning.contentops.domain.ContentConflictException;
import vn.englishlearning.contentops.domain.ContentValidationException;
import vn.englishlearning.contentops.domain.DryRunSummary;
import vn.englishlearning.contentops.domain.ImportSummary;
import vn.englishlearning.contentops.domain.ValidationResult;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.domain.ActivityType;
import vn.englishlearning.curriculum.domain.CompletionPolicy;
import vn.englishlearning.curriculum.domain.Curriculum;
import vn.englishlearning.curriculum.domain.CurriculumPhase;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.CurriculumWeek;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.domain.MediaType;
import vn.englishlearning.curriculum.domain.Resource;
import vn.englishlearning.curriculum.domain.RevisionStatus;
import vn.englishlearning.curriculum.domain.VerificationStatus;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumPhaseRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.CurriculumWeekRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.curriculum.infrastructure.ResourceRepository;
import vn.englishlearning.learning.domain.ProgressPolicy;

@Service
public class ImportCurriculumPackageService {

  private static final Logger log = LoggerFactory.getLogger(ImportCurriculumPackageService.class);

  private final ContentPackageValidator validator;
  private final PackageChecksumCalculator checksumCalculator;
  private final CurriculumRepository curriculumRepository;
  private final CurriculumRevisionRepository revisionRepository;
  private final CurriculumPhaseRepository phaseRepository;
  private final CurriculumWeekRepository weekRepository;
  private final LessonRepository lessonRepository;
  private final ActivityRepository activityRepository;
  private final ResourceRepository resourceRepository;
  private final QuizQuestionRepository quizQuestionRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ImportCurriculumPackageService(
      ContentPackageValidator validator,
      PackageChecksumCalculator checksumCalculator,
      CurriculumRepository curriculumRepository,
      CurriculumRevisionRepository revisionRepository,
      CurriculumPhaseRepository phaseRepository,
      CurriculumWeekRepository weekRepository,
      LessonRepository lessonRepository,
      ActivityRepository activityRepository,
      ResourceRepository resourceRepository,
      QuizQuestionRepository quizQuestionRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.validator = validator;
    this.checksumCalculator = checksumCalculator;
    this.curriculumRepository = curriculumRepository;
    this.revisionRepository = revisionRepository;
    this.phaseRepository = phaseRepository;
    this.weekRepository = weekRepository;
    this.lessonRepository = lessonRepository;
    this.activityRepository = activityRepository;
    this.resourceRepository = resourceRepository;
    this.quizQuestionRepository = quizQuestionRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /**
   * Dry-run inspection of a content package. Validates schemas and rules without writing to the
   * database.
   */
  public DryRunSummary dryRun(Path packageDir) {
    ValidationResult validation = validator.validate(packageDir);
    if (validation.hasErrors()) {
      throw new ContentValidationException(
          "Package validation failed with " + validation.errors().size() + " error(s)",
          validation.errors());
    }

    try {
      String checksum = checksumCalculator.calculate(packageDir);
      Path manifestPath = packageDir.resolve("manifest.json");
      JsonNode manifest = objectMapper.readTree(manifestPath.toFile());

      String curriculumKey = manifest.path("curriculumKey").asText();
      String version = manifest.path("version").asText();
      String title = manifest.path("title").asText();

      int phaseCount = 0;
      int weekCount = 0;
      int lessonCount = 0;
      int requiredLessonCount = 0;
      int optionalLessonCount = 0;
      int activityCount = 0;
      int effectivelyRequiredActivityCount = 0;

      JsonNode phases = manifest.path("phases");
      if (phases.isArray()) {
        phaseCount = phases.size();
        for (JsonNode phase : phases) {
          JsonNode weeks = phase.path("weeks");
          if (weeks.isArray()) {
            weekCount += weeks.size();
            for (JsonNode weekEntry : weeks) {
              String weekPathStr = weekEntry.path("path").asText();
              Path weekPath = packageDir.resolve(weekPathStr).normalize();
              JsonNode weekJson = objectMapper.readTree(weekPath.toFile());
              JsonNode lessons = weekJson.path("lessons");
              if (lessons.isArray()) {
                lessonCount += lessons.size();
                for (JsonNode lesson : lessons) {
                  boolean lReq = lesson.path("required").asBoolean(true);
                  if (lReq) {
                    requiredLessonCount++;
                  } else {
                    optionalLessonCount++;
                  }

                  JsonNode acts = lesson.path("activities");
                  if (acts.isArray()) {
                    activityCount += acts.size();
                    for (JsonNode act : acts) {
                      boolean aReq = act.path("required").asBoolean(true);
                      if (ProgressPolicy.isEffectivelyRequired(lReq, aReq)) {
                        effectivelyRequiredActivityCount++;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      int resourceCount = 0;
      int unverifiedResourceCount = 0;
      Path resPath = packageDir.resolve("resources.json");
      if (Files.exists(resPath)) {
        JsonNode resJson = objectMapper.readTree(resPath.toFile());
        JsonNode resArray = resJson.path("resources");
        if (resArray.isArray()) {
          resourceCount = resArray.size();
          for (JsonNode r : resArray) {
            if ("UNVERIFIED_DRAFT".equals(r.path("verificationStatus").asText())) {
              unverifiedResourceCount++;
            }
          }
        }
      }

      return new DryRunSummary(
          curriculumKey,
          version,
          title,
          phaseCount,
          weekCount,
          lessonCount,
          requiredLessonCount,
          optionalLessonCount,
          activityCount,
          effectivelyRequiredActivityCount,
          resourceCount,
          unverifiedResourceCount,
          checksum,
          validation.warnings());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read package files during dry-run", e);
    }
  }

  /**
   * Imports a validated curriculum package in a single database transaction with status DRAFT.
   * Re-importing identical package is idempotent. Content changes to an existing revision throw
   * ContentConflictException.
   */
  @Transactional
  public ImportSummary importPackage(Path packageDir) {
    ValidationResult validation = validator.validate(packageDir);
    if (validation.hasErrors()) {
      throw new ContentValidationException(
          "Package validation failed with " + validation.errors().size() + " error(s)",
          validation.errors());
    }

    String checksum;
    try {
      checksum = checksumCalculator.calculate(packageDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to calculate package checksum", e);
    }

    Path manifestPath = packageDir.resolve("manifest.json");
    JsonNode manifest;
    try {
      manifest = objectMapper.readTree(manifestPath.toFile());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read manifest.json", e);
    }

    String curriculumKey = manifest.path("curriculumKey").asText();
    String version = manifest.path("version").asText();
    String targetLocale = manifest.path("targetLocale").asText("en-GB");
    String title = manifest.path("title").asText();
    String description = manifest.path("description").asText();
    String authorsJson = manifest.path("authors").toString();
    String provenanceJson = manifest.path("provenance").toString();

    Instant now = Instant.now(clock);

    // 1. Find or create Curriculum
    Curriculum curriculum =
        curriculumRepository
            .findByCurriculumKey(curriculumKey)
            .orElseGet(
                () -> curriculumRepository.save(new Curriculum(curriculumKey, targetLocale, now)));

    // 2. Check for existing revision
    Optional<CurriculumRevision> existingRevisionOpt =
        revisionRepository.findByCurriculumIdAndVersion(curriculum.getId(), version);

    if (existingRevisionOpt.isPresent()) {
      CurriculumRevision existing = existingRevisionOpt.get();
      if (existing.getChecksum().equals(checksum)) {
        log.info(
            "Curriculum package {} v{} already imported with identical checksum: {}",
            curriculumKey,
            version,
            checksum);
        int phaseCount =
            phaseRepository.findByRevisionIdOrderByOrderIndexAsc(existing.getId()).size();
        int lessonCount = lessonRepository.findByWeekPhaseRevisionId(existing.getId()).size();
        int activityCount =
            activityRepository.findByLessonWeekPhaseRevisionId(existing.getId()).size();
        int resourceCount = resourceRepository.findByRevisionId(existing.getId()).size();

        return new ImportSummary(
            existing.getId(),
            curriculumKey,
            version,
            existing.getStatus().name(),
            checksum,
            phaseCount,
            0,
            lessonCount,
            activityCount,
            resourceCount,
            false);
      }

      if (existing.getStatus() == RevisionStatus.PUBLISHED
          || existing.getStatus() == RevisionStatus.ARCHIVED) {
        throw new ContentConflictException(
            "Cannot modify revision "
                + curriculumKey
                + " v"
                + version
                + " because it is already "
                + existing.getStatus()
                + ". Published revisions are strictly immutable.");
      }

      throw new ContentConflictException(
          "Revision "
              + curriculumKey
              + " v"
              + version
              + " already exists in DRAFT with a different checksum ("
              + existing.getChecksum()
              + " vs new "
              + checksum
              + "). To re-import, resolve the revision conflict or bump version.");
    }

    // 3. Create CurriculumRevision in DRAFT status
    CurriculumRevision revision =
        new CurriculumRevision(
            curriculum, version, title, description, authorsJson, provenanceJson, checksum, now);
    CurriculumRevision savedRevision = revisionRepository.save(revision);

    // 4. Import resources
    Map<String, Resource> resourceMap = new HashMap<>();
    Path resPath = packageDir.resolve("resources.json");
    if (Files.exists(resPath)) {
      try {
        JsonNode resJson = objectMapper.readTree(resPath.toFile());
        JsonNode resArray = resJson.path("resources");
        if (resArray.isArray()) {
          for (JsonNode r : resArray) {
            String rKey = r.path("key").asText();
            String rTitle = r.path("title").asText();
            String rUrl = r.path("url").asText();
            MediaType mediaType = MediaType.valueOf(r.path("mediaType").asText());
            String rLocale = r.path("targetLocale").asText("en-GB");
            String attribution =
                r.hasNonNull("attribution") ? r.path("attribution").asText() : null;
            String license = r.hasNonNull("license") ? r.path("license").asText() : null;
            VerificationStatus vStatus =
                VerificationStatus.valueOf(r.path("verificationStatus").asText());

            Resource resEntity =
                new Resource(
                    savedRevision,
                    rKey,
                    rTitle,
                    rUrl,
                    mediaType,
                    rLocale,
                    attribution,
                    license,
                    vStatus);
            Resource savedResource = resourceRepository.save(resEntity);
            resourceMap.put(rKey, savedResource);
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to read resources.json", e);
      }
    }

    // 5. Import Phases, Weeks, Lessons, Activities
    int totalPhases = 0;
    int totalWeeks = 0;
    int totalLessons = 0;
    int totalActivities = 0;

    JsonNode phases = manifest.path("phases");
    if (phases.isArray()) {
      totalPhases = phases.size();
      for (JsonNode phaseNode : phases) {
        String pKey = phaseNode.path("key").asText();
        String pTitle = phaseNode.path("title").asText();
        int pOrder = phaseNode.path("orderIndex").asInt();

        CurriculumPhase phase = new CurriculumPhase(savedRevision, pKey, pTitle, pOrder);
        CurriculumPhase savedPhase = phaseRepository.save(phase);

        JsonNode weeksNode = phaseNode.path("weeks");
        if (weeksNode.isArray()) {
          totalWeeks += weeksNode.size();
          for (JsonNode weekEntry : weeksNode) {
            String wPathStr = weekEntry.path("path").asText();
            Path weekPath = packageDir.resolve(wPathStr).normalize();

            JsonNode weekJson;
            try {
              weekJson = objectMapper.readTree(weekPath.toFile());
            } catch (IOException e) {
              throw new IllegalStateException("Failed to read week file: " + wPathStr, e);
            }

            String wKey = weekJson.path("key").asText();
            int wNumber = weekJson.path("weekNumber").asInt();
            int wOrder = weekJson.path("orderIndex").asInt();
            String wTitle = weekJson.path("title").asText();
            String wDesc = weekJson.path("description").asText();

            CurriculumWeek week =
                new CurriculumWeek(savedPhase, wKey, wNumber, wOrder, wTitle, wDesc);
            CurriculumWeek savedWeek = weekRepository.save(week);

            JsonNode lessonsNode = weekJson.path("lessons");
            if (lessonsNode.isArray()) {
              totalLessons += lessonsNode.size();
              for (JsonNode lessonNode : lessonsNode) {
                String lKey = lessonNode.path("key").asText();
                int lOrder = lessonNode.path("orderIndex").asInt();
                String lTitle = lessonNode.path("title").asText();
                int lEstMinutes = lessonNode.path("estimatedMinutes").asInt();
                boolean lReq = lessonNode.path("required").asBoolean(true);
                String contentFile = lessonNode.path("contentFile").asText();

                Path mdPath = packageDir.resolve(contentFile).normalize();
                String markdown;
                try {
                  markdown = Files.readString(mdPath, StandardCharsets.UTF_8);
                } catch (IOException e) {
                  throw new IllegalStateException("Cannot read lesson markdown: " + contentFile, e);
                }

                Lesson lesson =
                    new Lesson(savedWeek, lKey, lOrder, lTitle, lEstMinutes, lReq, markdown);
                Lesson savedLesson = lessonRepository.save(lesson);

                JsonNode actsNode = lessonNode.path("activities");
                if (actsNode.isArray()) {
                  totalActivities += actsNode.size();
                  for (int aIdx = 0; aIdx < actsNode.size(); aIdx++) {
                    JsonNode actNode = actsNode.get(aIdx);
                    String aKey = actNode.path("key").asText();
                    int aOrder =
                        actNode.hasNonNull("orderIndex")
                            ? actNode.path("orderIndex").asInt()
                            : (aIdx + 1);
                    String aTitle = actNode.path("title").asText();
                    ActivityType aType = ActivityType.valueOf(actNode.path("type").asText());
                    boolean aReq = actNode.path("required").asBoolean(true);
                    CompletionPolicy cPolicy =
                        CompletionPolicy.valueOf(actNode.path("completionPolicy").asText());
                    String instructions = actNode.path("instructions").asText();
                    String skillTagsJson =
                        actNode.hasNonNull("skillTags")
                            ? actNode.path("skillTags").toString()
                            : null;

                    Activity act =
                        new Activity(
                            savedLesson,
                            aKey,
                            aOrder,
                            aTitle,
                            aType,
                            aReq,
                            cPolicy,
                            instructions,
                            skillTagsJson);

                    JsonNode rKeys = actNode.path("resourceKeys");
                    if (rKeys.isArray()) {
                      for (JsonNode rk : rKeys) {
                        Resource r = resourceMap.get(rk.asText());
                        if (r != null) {
                          act.addResource(r);
                        }
                      }
                    }

                    Activity savedAct = activityRepository.save(act);

                    JsonNode qQuestionsNode = actNode.path("quizQuestions");
                    if (qQuestionsNode.isArray()) {
                      for (int qIdx = 0; qIdx < qQuestionsNode.size(); qIdx++) {
                        JsonNode qNode = qQuestionsNode.get(qIdx);
                        int qOrder =
                            qNode.hasNonNull("orderIndex")
                                ? qNode.path("orderIndex").asInt()
                                : (qIdx + 1);
                        String prompt = qNode.path("prompt").asText();
                        String explanation =
                            qNode.hasNonNull("explanation")
                                ? qNode.path("explanation").asText()
                                : null;
                        QuizQuestion question =
                            new QuizQuestion(savedAct.getId(), qOrder, prompt, explanation, now);

                        JsonNode optionsNode = qNode.path("options");
                        if (optionsNode.isArray()) {
                          for (int oIdx = 0; oIdx < optionsNode.size(); oIdx++) {
                            JsonNode oNode = optionsNode.get(oIdx);
                            int oOrder =
                                oNode.hasNonNull("orderIndex")
                                    ? oNode.path("orderIndex").asInt()
                                    : (oIdx + 1);
                            String content = oNode.path("content").asText();
                            boolean isCorrect = oNode.path("isCorrect").asBoolean(false);
                            QuizOption option = new QuizOption(oOrder, content, isCorrect, now);
                            question.addOption(option);
                          }
                        }
                        quizQuestionRepository.save(question);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    log.info(
        "Successfully imported curriculum package {} v{} (revisionId: {}) in status DRAFT",
        curriculumKey,
        version,
        savedRevision.getId());

    return new ImportSummary(
        savedRevision.getId(),
        curriculumKey,
        version,
        savedRevision.getStatus().name(),
        checksum,
        totalPhases,
        totalWeeks,
        totalLessons,
        totalActivities,
        resourceMap.size(),
        true);
  }
}
