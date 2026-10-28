package vn.englishlearning.contentops.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.englishlearning.contentops.application.ContentPackageValidator;
import vn.englishlearning.contentops.application.ImportCurriculumPackageService;
import vn.englishlearning.contentops.application.PackageChecksumCalculator;
import vn.englishlearning.contentops.domain.DryRunSummary;

class DryRunAndCliTest {

  @Test
  @DisplayName("Dry-run reports correct metrics for pilot curriculum without DB writes")
  void dryRun_pilotCurriculum_reportsExactMetrics() {
    Path packageDir = Paths.get("../content/pilot-curriculum/v1").normalize();
    ObjectMapper objectMapper = new ObjectMapper();
    ContentPackageValidator validator = new ContentPackageValidator(objectMapper);
    PackageChecksumCalculator checksumCalc = new PackageChecksumCalculator(objectMapper);
    ImportCurriculumPackageService service =
        new ImportCurriculumPackageService(
            validator,
            checksumCalc,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            objectMapper,
            null);

    DryRunSummary summary = service.dryRun(packageDir);

    assertThat(summary.curriculumKey()).isEqualTo("pilot-curriculum");
    assertThat(summary.version()).isEqualTo("1.0.0");
    assertThat(summary.phaseCount()).isEqualTo(1);
    assertThat(summary.weekCount()).isEqualTo(1);
    assertThat(summary.lessonCount()).isEqualTo(6);
    assertThat(summary.requiredLessonCount()).isEqualTo(5);
    assertThat(summary.optionalLessonCount()).isEqualTo(1);
    assertThat(summary.activityCount()).isEqualTo(22);
    assertThat(summary.effectivelyRequiredActivityCount()).isEqualTo(20);
    assertThat(summary.resourceCount()).isEqualTo(5);
    assertThat(summary.unverifiedResourceCount()).isEqualTo(5);
    assertThat(summary.checksum()).isNotNull().hasSize(64);
    assertThat(summary.warnings()).hasSize(5);
  }

  @Test
  @DisplayName("CLI validate command succeeds on valid pilot package")
  void cli_validate_succeeds() {
    int exitCode =
        CurriculumCli.execute(
            new String[] {"content", "validate", "../content/pilot-curriculum/v1"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("CLI dry-run command succeeds on valid pilot package")
  void cli_dryRun_succeeds() {
    int exitCode =
        CurriculumCli.execute(
            new String[] {"content", "dry-run", "../content/pilot-curriculum/v1"});
    assertThat(exitCode).isEqualTo(0);
  }

  @Test
  @DisplayName("CLI import rejects execution without explicit database confirmation")
  void cli_import_requiresExplicitDbConfirmation() {
    int exitCode =
        CurriculumCli.execute(new String[] {"content", "import", "../content/pilot-curriculum/v1"});
    assertThat(exitCode).isEqualTo(1);
  }

  @Test
  @DisplayName("CLI publish rejects execution without explicit database confirmation")
  void cli_publish_requiresExplicitDbConfirmation() {
    int exitCode =
        CurriculumCli.execute(new String[] {"content", "publish", "pilot-curriculum", "1.0.0"});
    assertThat(exitCode).isEqualTo(1);
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  static class NoDatabaseBeans {}

  @Test
  @DisplayName(
      "R1 Regression: Reviewer reproduction test - explicit properties override dev config without opening DB")
  void reviewExplicitCliDatabaseOverridesApplicationConfig() {
    String expected = "jdbc:postgresql://127.0.0.1:1/review_no_database_connection";
    java.util.concurrent.atomic.AtomicReference<String> actual =
        new java.util.concurrent.atomic.AtomicReference<>();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new org.springframework.boot.builder.SpringApplicationBuilder(NoDatabaseBeans.class)
                    .web(org.springframework.boot.WebApplicationType.NONE)
                    .properties(java.util.Map.of("spring.datasource.url", expected))
                    .initializers(
                        ctx -> {
                          actual.set(ctx.getEnvironment().getProperty("spring.datasource.url"));
                          throw new IllegalStateException(
                              "Review stopped before bean creation or any DB connection");
                        })
                    .run("content"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(actual.get())
        .as("Explicit CLI DB target must win; no DB was opened")
        .isEqualTo(expected);
  }

  @Test
  @DisplayName(
      "R1 Regression: CurriculumCli application builder ensures explicit DB properties override dev profile config")
  void explicitCliDatabaseOverridesDevProfileConfig() {
    String expectedUrl = "jdbc:postgresql://127.0.0.1:1/review_no_database_connection";
    String expectedUser = "cli_override_user";
    String expectedPass = "cli_override_pass";
    java.util.concurrent.atomic.AtomicReference<String> actualUrl =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<String> actualUser =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<String> actualPass =
        new java.util.concurrent.atomic.AtomicReference<>();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                CurriculumCli.createCliApplicationBuilder(
                        NoDatabaseBeans.class,
                        java.util.Map.of(
                            "spring.datasource.url", expectedUrl,
                            "spring.datasource.username", expectedUser,
                            "spring.datasource.password", expectedPass))
                    .initializers(
                        ctx -> {
                          actualUrl.set(ctx.getEnvironment().getProperty("spring.datasource.url"));
                          actualUser.set(
                              ctx.getEnvironment().getProperty("spring.datasource.username"));
                          actualPass.set(
                              ctx.getEnvironment().getProperty("spring.datasource.password"));
                          throw new IllegalStateException(
                              "Stopped before bean creation or any DB connection");
                        })
                    .run("content"))
        .isInstanceOf(IllegalStateException.class);

    assertThat(actualUrl.get())
        .as("Explicit CLI DB URL must win over dev profile config; no DB opened")
        .isEqualTo(expectedUrl);
    assertThat(actualUser.get()).isEqualTo(expectedUser);
    assertThat(actualPass.get()).isEqualTo(expectedPass);
  }
}
