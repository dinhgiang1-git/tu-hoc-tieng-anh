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

class PackageChecksumCalculatorTest {

  private PackageChecksumCalculator calculator;

  @BeforeEach
  void setUp() {
    calculator = new PackageChecksumCalculator(new ObjectMapper());
  }

  @Test
  @DisplayName("Checksum calculation is deterministic across multiple runs")
  void checksum_isDeterministic() throws IOException {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    String checksum1 = calculator.calculate(packageDir);
    String checksum2 = calculator.calculate(packageDir);

    assertThat(checksum1).isNotNull().hasSize(64);
    assertThat(checksum1).isEqualTo(checksum2);
  }

  @Test
  @DisplayName("Checksum changes when any markdown file content changes")
  void checksum_changesWhenMarkdownChanges(@TempDir Path tempDir) throws IOException {
    Path sourceDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    copyDirectory(sourceDir, tempDir);

    String originalChecksum = calculator.calculate(tempDir);

    // Modify one lesson markdown file
    Path mdFile = tempDir.resolve("lessons/w01-l01-vowels.md");
    Files.writeString(mdFile, Files.readString(mdFile) + "\n<!-- Minor modification -->");

    String modifiedChecksum = calculator.calculate(tempDir);

    assertThat(modifiedChecksum).isNotEqualTo(originalChecksum);
  }

  @Test
  @DisplayName("Checksum changes when resources.json content changes")
  void checksum_changesWhenResourcesChanges(@TempDir Path tempDir) throws IOException {
    Path sourceDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    copyDirectory(sourceDir, tempDir);

    String originalChecksum = calculator.calculate(tempDir);

    Path resFile = tempDir.resolve("resources.json");
    Files.writeString(
        resFile,
        Files.readString(resFile)
            .replace("res.bbc.vowels-i-long-short", "res.bbc.vowels-modified"));

    String modifiedChecksum = calculator.calculate(tempDir);

    assertThat(modifiedChecksum).isNotEqualTo(originalChecksum);
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
