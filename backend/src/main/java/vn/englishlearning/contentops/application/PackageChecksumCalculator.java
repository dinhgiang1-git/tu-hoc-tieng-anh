package vn.englishlearning.contentops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PackageChecksumCalculator {

  private final ObjectMapper objectMapper;

  public PackageChecksumCalculator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String calculate(Path packageDir) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      Path realPackageDir = packageDir.toRealPath();

      Path manifestPath = validateSafeFile(packageDir, realPackageDir, "manifest.json");
      updateDigestWithFile(digest, manifestPath);

      JsonNode manifestNode = objectMapper.readTree(manifestPath.toFile());

      Path resourcesPath = packageDir.resolve("resources.json");
      if (Files.exists(resourcesPath)) {
        Path safeResPath = validateSafeFile(packageDir, realPackageDir, "resources.json");
        updateDigestWithFile(digest, safeResPath);
      }

      JsonNode phasesNode = manifestNode.get("phases");
      if (phasesNode != null && phasesNode.isArray()) {
        for (JsonNode phaseNode : phasesNode) {
          JsonNode weeksNode = phaseNode.get("weeks");
          if (weeksNode != null && weeksNode.isArray()) {
            for (JsonNode weekEntry : weeksNode) {
              String weekRelPath = weekEntry.path("path").asText();
              Path safeWeekPath = validateSafeFile(packageDir, realPackageDir, weekRelPath);
              if (Files.exists(safeWeekPath)) {
                updateDigestWithFile(digest, safeWeekPath);
                JsonNode weekNode = objectMapper.readTree(safeWeekPath.toFile());
                JsonNode lessonsNode = weekNode.get("lessons");
                if (lessonsNode != null && lessonsNode.isArray()) {
                  for (JsonNode lessonNode : lessonsNode) {
                    String contentFile = lessonNode.path("contentFile").asText();
                    if (contentFile != null && !contentFile.isBlank()) {
                      Path safeMdPath = validateSafeFile(packageDir, realPackageDir, contentFile);
                      if (Files.exists(safeMdPath)) {
                        updateDigestWithFile(digest, safeMdPath);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      byte[] hashBytes = digest.digest();
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private void updateDigestWithFile(MessageDigest digest, Path path) throws IOException {
    String content = Files.readString(path, StandardCharsets.UTF_8);
    // Normalize CRLF to LF for deterministic hashes across operating systems
    String normalized = content.replace("\r\n", "\n");
    digest.update(normalized.getBytes(StandardCharsets.UTF_8));
  }

  private Path validateSafeFile(Path packageDir, Path realPackageDir, String relativePathStr)
      throws IOException {
    Path path = packageDir.resolve(relativePathStr).normalize();
    if (!Files.exists(path)) {
      return path;
    }
    Path realPath = path.toRealPath();
    if (!realPath.startsWith(realPackageDir)) {
      throw new SecurityException(
          "Path points outside package directory (symlink forbidden): " + relativePathStr);
    }
    return realPath;
  }
}
