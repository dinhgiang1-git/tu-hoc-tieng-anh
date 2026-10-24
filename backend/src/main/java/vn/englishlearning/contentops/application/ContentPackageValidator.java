package vn.englishlearning.contentops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import vn.englishlearning.contentops.domain.ValidationError;
import vn.englishlearning.contentops.domain.ValidationResult;
import vn.englishlearning.contentops.domain.ValidationWarning;

@Component
public class ContentPackageValidator {

  private final ObjectMapper objectMapper;
  private final JsonSchema manifestSchema;
  private final JsonSchema weekSchema;
  private final JsonSchema resourcesSchema;

  public ContentPackageValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    this.manifestSchema = loadSchema(factory, "/content/schemas/manifest.schema.json");
    this.weekSchema = loadSchema(factory, "/content/schemas/week.schema.json");
    this.resourcesSchema = loadSchema(factory, "/content/schemas/resources.schema.json");
  }

  private JsonSchema loadSchema(JsonSchemaFactory factory, String classpathLocation) {
    try (InputStream is = getClass().getResourceAsStream(classpathLocation)) {
      if (is == null) {
        throw new IllegalStateException("Schema resource not found: " + classpathLocation);
      }
      return factory.getSchema(is);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load schema from " + classpathLocation, e);
    }
  }

  public ValidationResult validate(Path packageDir) {
    List<ValidationError> errors = new ArrayList<>();
    List<ValidationWarning> warnings = new ArrayList<>();

    if (packageDir == null || !Files.exists(packageDir) || !Files.isDirectory(packageDir)) {
      errors.add(
          new ValidationError(
              "package",
              "",
              "Package directory does not exist or is not a directory: " + packageDir));
      return ValidationResult.failure(errors, warnings);
    }

    Path realPackageDir;
    try {
      realPackageDir = packageDir.toRealPath();
    } catch (IOException e) {
      errors.add(
          new ValidationError(
              "package", "", "Cannot resolve package directory real path: " + e.getMessage()));
      return ValidationResult.failure(errors, warnings);
    }

    // 1. Validate manifest.json
    Path manifestPath = validateSafeRootFile(packageDir, realPackageDir, "manifest.json", errors);
    if (manifestPath == null) {
      return ValidationResult.failure(errors, warnings);
    }

    JsonNode manifestNode;
    try {
      manifestNode = objectMapper.readTree(manifestPath.toFile());
    } catch (IOException e) {
      errors.add(
          new ValidationError("manifest.json", "", "Invalid JSON syntax: " + e.getMessage()));
      return ValidationResult.failure(errors, warnings);
    }

    Set<ValidationMessage> manifestMessages = manifestSchema.validate(manifestNode);
    for (ValidationMessage msg : manifestMessages) {
      errors.add(
          new ValidationError(
              "manifest.json", msg.getInstanceLocation().toString(), msg.getMessage()));
    }

    // 2. Validate resources.json
    Path resourcesPath = validateSafeRootFile(packageDir, realPackageDir, "resources.json", errors);
    JsonNode resourcesNode = null;
    Set<String> declaredResourceKeys = new HashSet<>();

    if (resourcesPath != null) {
      try {
        resourcesNode = objectMapper.readTree(resourcesPath.toFile());
        Set<ValidationMessage> resMessages = resourcesSchema.validate(resourcesNode);
        for (ValidationMessage msg : resMessages) {
          errors.add(
              new ValidationError(
                  "resources.json", msg.getInstanceLocation().toString(), msg.getMessage()));
        }

        // Cross-check curriculumKey & version with manifest
        if (manifestNode.hasNonNull("curriculumKey")
            && resourcesNode.hasNonNull("curriculumKey")
            && !manifestNode
                .get("curriculumKey")
                .asText()
                .equals(resourcesNode.get("curriculumKey").asText())) {
          errors.add(
              new ValidationError(
                  "resources.json",
                  "$.curriculumKey",
                  "curriculumKey does not match manifest.json ('"
                      + resourcesNode.get("curriculumKey").asText()
                      + "' vs '"
                      + manifestNode.get("curriculumKey").asText()
                      + "')"));
        }

        if (manifestNode.hasNonNull("version")
            && resourcesNode.hasNonNull("version")
            && !manifestNode
                .get("version")
                .asText()
                .equals(resourcesNode.get("version").asText())) {
          errors.add(
              new ValidationError(
                  "resources.json",
                  "$.version",
                  "version does not match manifest.json ('"
                      + resourcesNode.get("version").asText()
                      + "' vs '"
                      + manifestNode.get("version").asText()
                      + "')"));
        }

        // Validate individual resources & collect keys
        JsonNode resArray = resourcesNode.get("resources");
        if (resArray != null && resArray.isArray()) {
          for (int i = 0; i < resArray.size(); i++) {
            JsonNode r = resArray.get(i);
            String rKey = r.path("key").asText();
            if (rKey != null && !rKey.isBlank()) {
              if (!declaredResourceKeys.add(rKey)) {
                errors.add(
                    new ValidationError(
                        "resources.json",
                        "$.resources[" + i + "].key",
                        "Duplicate resource key: " + rKey));
              }
            }
            String vStatus = r.path("verificationStatus").asText();
            if ("UNVERIFIED_DRAFT".equals(vStatus)) {
              warnings.add(
                  new ValidationWarning(
                      "resources.json",
                      "$.resources[" + i + "]",
                      "Resource '" + rKey + "' is UNVERIFIED_DRAFT"));
            }
          }
        }
      } catch (IOException e) {
        errors.add(
            new ValidationError("resources.json", "", "Invalid JSON syntax: " + e.getMessage()));
      }
    }

    // 3. Traverse phases and referenced weeks
    Set<String> phaseKeys = new HashSet<>();
    Set<Integer> phaseOrders = new HashSet<>();
    Set<String> weekKeys = new HashSet<>();
    Set<String> lessonKeys = new HashSet<>();
    Set<String> activityKeys = new HashSet<>();
    Set<String> referencedResourceKeys = new HashSet<>();

    JsonNode phasesNode = manifestNode.get("phases");
    if (phasesNode != null && phasesNode.isArray()) {
      for (int pIdx = 0; pIdx < phasesNode.size(); pIdx++) {
        JsonNode phase = phasesNode.get(pIdx);
        String pKey = phase.path("key").asText();
        int pOrder = phase.path("orderIndex").asInt(-1);

        if (pKey != null && !pKey.isBlank()) {
          if (!phaseKeys.add(pKey)) {
            errors.add(
                new ValidationError(
                    "manifest.json", "$.phases[" + pIdx + "].key", "Duplicate phase key: " + pKey));
          }
        }
        if (pOrder > 0) {
          if (!phaseOrders.add(pOrder)) {
            errors.add(
                new ValidationError(
                    "manifest.json",
                    "$.phases[" + pIdx + "].orderIndex",
                    "Duplicate phase orderIndex: " + pOrder));
          }
        }

        JsonNode weeksNode = phase.get("weeks");
        if (weeksNode != null && weeksNode.isArray()) {
          for (int wIdx = 0; wIdx < weeksNode.size(); wIdx++) {
            JsonNode weekEntry = weeksNode.get(wIdx);
            String wKey = weekEntry.path("key").asText();
            String wRelPath = weekEntry.path("path").asText();

            if (wKey != null && !wKey.isBlank()) {
              if (!weekKeys.add(wKey)) {
                errors.add(
                    new ValidationError(
                        "manifest.json",
                        "$.phases[" + pIdx + "].weeks[" + wIdx + "].key",
                        "Duplicate week key in manifest: " + wKey));
              }
            }

            // Path containment and traversal validation
            Path weekFile =
                validateSafeRelativePath(
                    packageDir,
                    realPackageDir,
                    wRelPath,
                    "manifest.json",
                    "$.phases[" + pIdx + "].weeks[" + wIdx + "].path",
                    errors);
            if (weekFile == null) {
              continue;
            }

            if (!Files.exists(weekFile) || !Files.isRegularFile(weekFile)) {
              errors.add(
                  new ValidationError(
                      "manifest.json",
                      "$.phases[" + pIdx + "].weeks[" + wIdx + "].path",
                      "Referenced week file does not exist: " + wRelPath));
              continue;
            }

            // Validate week schema
            JsonNode weekNode;
            try {
              weekNode = objectMapper.readTree(weekFile.toFile());
            } catch (IOException e) {
              errors.add(
                  new ValidationError(wRelPath, "", "Invalid JSON syntax: " + e.getMessage()));
              continue;
            }

            Set<ValidationMessage> weekMessages = weekSchema.validate(weekNode);
            for (ValidationMessage msg : weekMessages) {
              errors.add(
                  new ValidationError(
                      wRelPath, msg.getInstanceLocation().toString(), msg.getMessage()));
            }

            String fileWeekKey = weekNode.path("key").asText();
            if (wKey != null && !wKey.equals(fileWeekKey)) {
              errors.add(
                  new ValidationError(
                      wRelPath,
                      "$.key",
                      "Week key in file ('"
                          + fileWeekKey
                          + "') does not match manifest reference ('"
                          + wKey
                          + "')"));
            }

            // Validate lessons in week
            JsonNode lessonsNode = weekNode.get("lessons");
            Set<Integer> lessonOrders = new HashSet<>();
            if (lessonsNode != null && lessonsNode.isArray()) {
              for (int lIdx = 0; lIdx < lessonsNode.size(); lIdx++) {
                JsonNode lesson = lessonsNode.get(lIdx);
                String lKey = lesson.path("key").asText();
                int lOrder = lesson.path("orderIndex").asInt(-1);
                String contentFile = lesson.path("contentFile").asText();

                if (lKey != null && !lKey.isBlank()) {
                  if (!lessonKeys.add(lKey)) {
                    errors.add(
                        new ValidationError(
                            wRelPath,
                            "$.lessons[" + lIdx + "].key",
                            "Duplicate lesson key: " + lKey));
                  }
                }
                if (lOrder > 0) {
                  if (!lessonOrders.add(lOrder)) {
                    errors.add(
                        new ValidationError(
                            wRelPath,
                            "$.lessons[" + lIdx + "].orderIndex",
                            "Duplicate lesson orderIndex within week: " + lOrder));
                  }
                }

                // Check markdown file path security and existence
                Path mdPath =
                    validateSafeRelativePath(
                        packageDir,
                        realPackageDir,
                        contentFile,
                        wRelPath,
                        "$.lessons[" + lIdx + "].contentFile",
                        errors);
                if (mdPath != null) {
                  if (!Files.exists(mdPath) || !Files.isRegularFile(mdPath)) {
                    errors.add(
                        new ValidationError(
                            wRelPath,
                            "$.lessons[" + lIdx + "].contentFile",
                            "Lesson markdown file does not exist: " + contentFile));
                  } else {
                    try {
                      String mdContent = Files.readString(mdPath);
                      if (mdContent.trim().isEmpty()) {
                        errors.add(
                            new ValidationError(
                                wRelPath,
                                "$.lessons[" + lIdx + "].contentFile",
                                "Lesson markdown file is empty: " + contentFile));
                      }
                    } catch (IOException e) {
                      errors.add(
                          new ValidationError(
                              wRelPath,
                              "$.lessons[" + lIdx + "].contentFile",
                              "Cannot read lesson markdown file: " + e.getMessage()));
                    }
                  }
                }

                // Validate activities in lesson
                JsonNode activitiesNode = lesson.get("activities");
                Set<Integer> actOrders = new HashSet<>();
                if (activitiesNode != null && activitiesNode.isArray()) {
                  for (int aIdx = 0; aIdx < activitiesNode.size(); aIdx++) {
                    JsonNode act = activitiesNode.get(aIdx);
                    String aKey = act.path("key").asText();
                    int aOrder = act.path("orderIndex").asInt(aIdx + 1);

                    if (aKey != null && !aKey.isBlank()) {
                      if (!activityKeys.add(aKey)) {
                        errors.add(
                            new ValidationError(
                                wRelPath,
                                "$.lessons[" + lIdx + "].activities[" + aIdx + "].key",
                                "Duplicate activity key: " + aKey));
                      }
                    }
                    if (aOrder > 0) {
                      if (!actOrders.add(aOrder)) {
                        errors.add(
                            new ValidationError(
                                wRelPath,
                                "$.lessons[" + lIdx + "].activities[" + aIdx + "].orderIndex",
                                "Duplicate activity orderIndex within lesson: " + aOrder));
                      }
                    }

                    // Check resource references
                    JsonNode rKeysNode = act.get("resourceKeys");
                    if (rKeysNode != null && rKeysNode.isArray()) {
                      for (int rkIdx = 0; rkIdx < rKeysNode.size(); rkIdx++) {
                        String rk = rKeysNode.get(rkIdx).asText();
                        referencedResourceKeys.add(rk);
                        if (!declaredResourceKeys.contains(rk)) {
                          errors.add(
                              new ValidationError(
                                  wRelPath,
                                  "$.lessons["
                                      + lIdx
                                      + "].activities["
                                      + aIdx
                                      + "].resourceKeys["
                                      + rkIdx
                                      + "]",
                                  "Referenced resource key not found in resources.json: " + rk));
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
    }

    if (errors.isEmpty()) {
      return ValidationResult.success(warnings);
    } else {
      return ValidationResult.failure(errors, warnings);
    }
  }

  private Path validateSafeRelativePath(
      Path packageDir,
      Path realPackageDir,
      String relativePathStr,
      String sourceFile,
      String fieldPath,
      List<ValidationError> errors) {
    if (relativePathStr == null || relativePathStr.isBlank()) {
      errors.add(new ValidationError(sourceFile, fieldPath, "Path must not be blank"));
      return null;
    }

    // Reject absolute paths
    if (relativePathStr.startsWith("/")
        || relativePathStr.startsWith("\\")
        || relativePathStr.matches("^[a-zA-Z]:.*")) {
      errors.add(
          new ValidationError(
              sourceFile, fieldPath, "Absolute paths are forbidden: " + relativePathStr));
      return null;
    }

    // Reject traversal patterns
    for (Path part : Path.of(relativePathStr)) {
      String p = part.toString();
      if (p.equals("..") || p.equals(".")) {
        errors.add(
            new ValidationError(
                sourceFile,
                fieldPath,
                "Path traversal tokens ('..', '.') are forbidden: " + relativePathStr));
        return null;
      }
    }

    Path resolved = packageDir.resolve(relativePathStr).normalize();
    try {
      Path realResolved = resolved.toRealPath();
      if (!realResolved.startsWith(realPackageDir)) {
        errors.add(
            new ValidationError(
                sourceFile,
                fieldPath,
                "Path points outside package directory (symlink or escape): " + relativePathStr));
        return null;
      }
    } catch (IOException ignored) {
      // If file does not exist yet, verify normalized path prefix
      if (!resolved.startsWith(packageDir.normalize())) {
        errors.add(
            new ValidationError(
                sourceFile,
                fieldPath,
                "Path resolves outside package directory: " + relativePathStr));
        return null;
      }
    }

    return resolved;
  }

  private Path validateSafeRootFile(
      Path packageDir, Path realPackageDir, String fileName, List<ValidationError> errors) {
    Path path = packageDir.resolve(fileName);
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      errors.add(new ValidationError(fileName, "", fileName + " file is missing"));
      return null;
    }
    try {
      Path realPath = path.toRealPath();
      if (!realPath.startsWith(realPackageDir)) {
        errors.add(
            new ValidationError(
                fileName, "", fileName + " points outside package directory (symlink forbidden)"));
        return null;
      }
      return realPath;
    } catch (IOException e) {
      errors.add(new ValidationError(fileName, "", "Cannot resolve real path: " + e.getMessage()));
      return null;
    }
  }
}
