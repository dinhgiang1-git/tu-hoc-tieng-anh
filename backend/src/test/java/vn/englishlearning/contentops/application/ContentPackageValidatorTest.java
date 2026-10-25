package vn.englishlearning.contentops.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.englishlearning.contentops.domain.ValidationResult;

class ContentPackageValidatorTest {

  private ContentPackageValidator validator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    validator = new ContentPackageValidator(objectMapper);
  }

  @Test
  @DisplayName("Validates the real pilot-curriculum v1 package successfully")
  void validate_realPilotCurriculum_success() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    assertThat(Files.exists(packageDir)).isTrue();

    ValidationResult result = validator.validate(packageDir);

    assertThat(result.hasErrors()).isFalse();
    assertThat(result.valid()).isTrue();
    // 5 resources in pilot curriculum are UNVERIFIED_DRAFT, so 5 warnings expected
    assertThat(result.warnings()).hasSize(5);
  }

  @Test
  @DisplayName("Rejects package when referenced markdown file is missing")
  void validate_missingMarkdownFile_fails(@TempDir Path tempDir) throws IOException {
    copyDirectory(Paths.get("../content/pilot-curriculum/v1").normalize(), tempDir);

    // Delete one markdown file
    Path mdToDelete = tempDir.resolve("lessons/w01-l01-vowels.md");
    Files.delete(mdToDelete);

    ValidationResult result = validator.validate(tempDir);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors())
        .anyMatch(e -> e.message().contains("Lesson markdown file does not exist"));
  }

  @Test
  @DisplayName("Rejects package containing directory traversal in week path")
  void validate_traversalInWeekPath_fails(@TempDir Path tempDir) throws IOException {
    copyDirectory(Paths.get("../content/pilot-curriculum/v1").normalize(), tempDir);

    Path manifestPath = tempDir.resolve("manifest.json");
    String manifestContent = Files.readString(manifestPath);
    String hacked = manifestContent.replace("weeks/week-01.json", "../weeks/week-01.json");
    Files.writeString(manifestPath, hacked);

    ValidationResult result = validator.validate(tempDir);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors()).anyMatch(e -> e.message().contains("traversal"));
  }

  @Test
  @DisplayName("Rejects package containing absolute path in markdown reference")
  void validate_absolutePath_fails(@TempDir Path tempDir) throws IOException {
    copyDirectory(Paths.get("../content/pilot-curriculum/v1").normalize(), tempDir);

    Path weekPath = tempDir.resolve("weeks/week-01.json");
    String weekContent = Files.readString(weekPath);
    String hacked = weekContent.replace("lessons/w01-l01-vowels.md", "/etc/passwd");
    Files.writeString(weekPath, hacked);

    ValidationResult result = validator.validate(tempDir);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors())
        .anyMatch(
            e ->
                e.message().contains("Absolute paths are forbidden")
                    || e.message().contains("pattern"));
  }

  @Test
  @DisplayName("Rejects package when activity references unlisted resource key")
  void validate_unlistedResourceKey_fails(@TempDir Path tempDir) throws IOException {
    copyDirectory(Paths.get("../content/pilot-curriculum/v1").normalize(), tempDir);

    Path weekPath = tempDir.resolve("weeks/week-01.json");
    String weekContent = Files.readString(weekPath);
    String hacked = weekContent.replace("res.bbc.vowels-i-long-short", "res.nonexistent.key");
    Files.writeString(weekPath, hacked);

    ValidationResult result = validator.validate(tempDir);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors())
        .anyMatch(e -> e.message().contains("Referenced resource key not found in resources.json"));
  }

  @Test
  @DisplayName("Rejects duplicate activity keys across lessons")
  void validate_duplicateActivityKeys_fails(@TempDir Path tempDir) throws IOException {
    copyDirectory(Paths.get("../content/pilot-curriculum/v1").normalize(), tempDir);

    Path weekPath = tempDir.resolve("weeks/week-01.json");
    String weekContent = Files.readString(weekPath);
    // Duplicate pilot.w01.l01.a01 in lesson 2
    String hacked = weekContent.replace("pilot.w01.l02.a01", "pilot.w01.l01.a01");
    Files.writeString(weekPath, hacked);

    ValidationResult result = validator.validate(tempDir);

    assertThat(result.hasErrors()).isTrue();
    assertThat(result.errors()).anyMatch(e -> e.message().contains("Duplicate activity key"));
  }

  @Test
  @DisplayName("Rejects package when root resources.json is a symlink pointing outside package")
  void rootResourcesSymlinkMustBeRejected(@TempDir Path dir) throws Exception {
    Path source = Paths.get("../content/pilot-curriculum/v1").toRealPath();
    Path pkg = dir.resolve("package");
    Files.createDirectories(pkg);
    copyDirectory(source, pkg);

    Path outside = dir.resolve("outside-resources.json");
    Files.copy(pkg.resolve("resources.json"), outside);
    Files.delete(pkg.resolve("resources.json"));
    try {
      Files.createSymbolicLink(pkg.resolve("resources.json"), outside);
    } catch (Exception e) {
      org.junit.jupiter.api.Assumptions.abort(
          "OS environment does not support creating symbolic links: " + e.getMessage());
    }

    ValidationResult result = validator.validate(pkg);
    assertThat(result.hasErrors())
        .as("Root resources.json symlink pointing outside package must be rejected")
        .isTrue();
    assertThat(result.errors())
        .anyMatch(err -> err.message().contains("symlink") || err.message().contains("outside"));
  }

  @Test
  @DisplayName("Rejects package when root manifest.json is a symlink pointing outside package")
  void rootManifestSymlinkMustBeRejected(@TempDir Path dir) throws Exception {
    Path source = Paths.get("../content/pilot-curriculum/v1").toRealPath();
    Path pkg = dir.resolve("package");
    Files.createDirectories(pkg);
    copyDirectory(source, pkg);

    Path outside = dir.resolve("outside-manifest.json");
    Files.copy(pkg.resolve("manifest.json"), outside);
    Files.delete(pkg.resolve("manifest.json"));
    try {
      Files.createSymbolicLink(pkg.resolve("manifest.json"), outside);
    } catch (Exception e) {
      org.junit.jupiter.api.Assumptions.abort(
          "OS environment does not support creating symbolic links: " + e.getMessage());
    }

    ValidationResult result = validator.validate(pkg);
    assertThat(result.hasErrors())
        .as("Root manifest.json symlink pointing outside package must be rejected")
        .isTrue();
    assertThat(result.errors())
        .anyMatch(err -> err.message().contains("symlink") || err.message().contains("outside"));
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    try (var stream = Files.walk(source)) {
      stream.forEach(
          src -> {
            try {
              Path dest = target.resolve(source.relativize(src));
              if (Files.isDirectory(src)) {
                if (!Files.exists(dest)) {
                  Files.createDirectories(dest);
                }
              } else {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }
}
