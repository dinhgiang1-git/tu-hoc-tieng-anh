package vn.englishlearning.contentops.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import vn.englishlearning.EnglishLearningApplication;
import vn.englishlearning.contentops.application.ContentPackageValidator;
import vn.englishlearning.contentops.application.ImportCurriculumPackageService;
import vn.englishlearning.contentops.application.PackageChecksumCalculator;
import vn.englishlearning.contentops.application.PublishCurriculumRevisionService;
import vn.englishlearning.contentops.domain.DryRunSummary;
import vn.englishlearning.contentops.domain.ImportSummary;
import vn.englishlearning.contentops.domain.PublishResult;
import vn.englishlearning.contentops.domain.ValidationError;
import vn.englishlearning.contentops.domain.ValidationResult;
import vn.englishlearning.contentops.domain.ValidationWarning;

public class CurriculumCli {

  public static void main(String[] args) {
    int exitCode = execute(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  public static int execute(String[] args) {
    if (args == null || args.length == 0) {
      printUsage();
      return 1;
    }

    // Skip "content" prefix if passed (e.g. from EnglishLearningApplication)
    String[] cliArgs = args;
    if ("content".equalsIgnoreCase(args[0])) {
      cliArgs = Arrays.copyOfRange(args, 1, args.length);
    }

    if (cliArgs.length == 0) {
      printUsage();
      return 1;
    }

    String command = cliArgs[0].toLowerCase();
    String[] cmdArgs = Arrays.copyOfRange(cliArgs, 1, cliArgs.length);

    try {
      return switch (command) {
        case "validate" -> handleValidate(cmdArgs);
        case "dry-run" -> handleDryRun(cmdArgs);
        case "import" -> handleImport(cmdArgs);
        case "publish" -> handlePublish(cmdArgs);
        case "help", "--help", "-h" -> {
          printUsage();
          yield 0;
        }
        default -> {
          System.err.println("Unknown command: " + command);
          printUsage();
          yield 1;
        }
      };
    } catch (Exception e) {
      System.err.println("Execution failed: " + e.getMessage());
      return 1;
    }
  }

  private static int handleValidate(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: content validate <package-dir>");
      return 1;
    }

    Path packageDir = Paths.get(args[0]);
    System.out.println("Validating content package at: " + packageDir.toAbsolutePath());

    ObjectMapper objectMapper = new ObjectMapper();
    ContentPackageValidator validator = new ContentPackageValidator(objectMapper);
    ValidationResult result = validator.validate(packageDir);

    if (result.hasWarnings()) {
      System.out.println("\nWarnings (" + result.warnings().size() + "):");
      for (ValidationWarning warning : result.warnings()) {
        System.out.println("  [WARN] " + warning);
      }
    }

    if (result.hasErrors()) {
      System.err.println("\nValidation FAILED with " + result.errors().size() + " error(s):");
      for (ValidationError error : result.errors()) {
        System.err.println("  [ERROR] " + error);
      }
      return 1;
    }

    System.out.println(
        "\nValidation SUCCESSFUL: All schemas, keys, references, and files are valid.");
    return 0;
  }

  private static int handleDryRun(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: content dry-run <package-dir>");
      return 1;
    }

    Path packageDir = Paths.get(args[0]);
    System.out.println("Executing dry-run on content package: " + packageDir.toAbsolutePath());

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

    System.out.println("============================================================");
    System.out.println("                CONTENT PACKAGE DRY-RUN REPORT              ");
    System.out.println("============================================================");
    System.out.println("Curriculum Key : " + summary.curriculumKey());
    System.out.println("Version        : " + summary.version());
    System.out.println("Title          : " + summary.title());
    System.out.println("Package Checksum (SHA-256): " + summary.checksum());
    System.out.println("------------------------------------------------------------");
    System.out.println("Phases         : " + summary.phaseCount());
    System.out.println("Weeks          : " + summary.weekCount());
    System.out.println(
        "Lessons        : "
            + summary.lessonCount()
            + " ("
            + summary.requiredLessonCount()
            + " required, "
            + summary.optionalLessonCount()
            + " optional)");
    System.out.println(
        "Activities     : "
            + summary.activityCount()
            + " ("
            + summary.effectivelyRequiredActivityCount()
            + " effectively required)");
    System.out.println(
        "Resources      : "
            + summary.resourceCount()
            + " ("
            + summary.unverifiedResourceCount()
            + " unverified draft)");
    System.out.println("------------------------------------------------------------");

    if (!summary.warnings().isEmpty()) {
      System.out.println("Warnings (" + summary.warnings().size() + "):");
      for (ValidationWarning w : summary.warnings()) {
        System.out.println("  * " + w);
      }
    }
    System.out.println("Database Status: No database writes performed (Dry-run mode).");
    System.out.println("============================================================");
    return 0;
  }

  private static int handleImport(String[] args) {
    if (args.length < 1) {
      System.err.println(
          "Usage: content import <package-dir> [--confirm-db] [--db-url <url>] [--db-user <user>] [--db-password <pass>]");
      return 1;
    }

    Path packageDir = Paths.get(args[0]);
    Map<String, Object> dbProps = extractExplicitDbProps(args);
    if (dbProps == null) {
      return 1;
    }

    System.out.println("Importing package from: " + packageDir.toAbsolutePath());

    try (ConfigurableApplicationContext ctx = createSpringCliContext(dbProps)) {
      ImportCurriculumPackageService service = ctx.getBean(ImportCurriculumPackageService.class);
      ImportSummary summary = service.importPackage(packageDir);

      System.out.println("============================================================");
      System.out.println("                 CONTENT IMPORT COMPLETED                   ");
      System.out.println("============================================================");
      System.out.println("Revision ID    : " + summary.revisionId());
      System.out.println("Curriculum Key : " + summary.curriculumKey());
      System.out.println("Version        : " + summary.version());
      System.out.println("Status         : " + summary.status());
      System.out.println("Newly Imported : " + summary.newlyImported());
      System.out.println("Checksum       : " + summary.checksum());
      System.out.println("Phases Imported: " + summary.phaseCount());
      System.out.println("Weeks Imported : " + summary.weekCount());
      System.out.println("Lessons        : " + summary.lessonCount());
      System.out.println("Activities     : " + summary.activityCount());
      System.out.println("Resources      : " + summary.resourceCount());
      System.out.println("============================================================");
      return 0;
    }
  }

  private static int handlePublish(String[] args) {
    if (args.length < 2) {
      System.err.println(
          "Usage: content publish <curriculumKey> <version> [--confirm-db] [--db-url <url>]");
      return 1;
    }

    String curriculumKey = args[0];
    String version = args[1];

    Map<String, Object> dbProps = extractExplicitDbProps(args);
    if (dbProps == null) {
      return 1;
    }

    System.out.println("Publishing curriculum revision: " + curriculumKey + " v" + version);

    try (ConfigurableApplicationContext ctx = createSpringCliContext(dbProps)) {
      PublishCurriculumRevisionService service =
          ctx.getBean(PublishCurriculumRevisionService.class);
      PublishResult result = service.publish(curriculumKey, version);

      System.out.println("============================================================");
      System.out.println("                 CONTENT PUBLISH COMPLETED                  ");
      System.out.println("============================================================");
      System.out.println("Revision ID    : " + result.revisionId());
      System.out.println("Curriculum Key : " + result.curriculumKey());
      System.out.println("Version        : " + result.version());
      System.out.println("Status         : " + result.status() + " (IMMUTABLE)");
      System.out.println("Published At   : " + result.publishedAt());
      if (!result.warnings().isEmpty()) {
        System.out.println("Warnings (" + result.warnings().size() + "):");
        for (String w : result.warnings()) {
          System.out.println("  * [WARN] " + w);
        }
      }
      System.out.println("============================================================");
      return 0;
    }
  }

  private static Map<String, Object> extractExplicitDbProps(String[] args) {
    Map<String, Object> props = new HashMap<>();
    boolean confirmed = false;

    String envDbUrl = System.getenv("CONTENT_DB_URL");
    if (envDbUrl != null && !envDbUrl.isBlank()) {
      props.put("spring.datasource.url", envDbUrl);
      confirmed = true;
    }

    for (int i = 0; i < args.length; i++) {
      if ("--confirm-db".equalsIgnoreCase(args[i])) {
        confirmed = true;
      } else if ("--db-url".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
        props.put("spring.datasource.url", args[++i]);
        confirmed = true;
      } else if ("--db-user".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
        props.put("spring.datasource.username", args[++i]);
      } else if ("--db-password".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
        props.put("spring.datasource.password", args[++i]);
      }
    }

    if (!confirmed) {
      System.err.println("SECURITY CHECK FAILED:");
      System.err.println(
          "Explicit database target configuration is required for import and publish.");
      System.err.println(
          "To prevent accidental mutation of the development database, provide one of:");
      System.err.println("  1) --db-url <jdbc-url> [--db-user <user>] [--db-password <pass>]");
      System.err.println("  2) CONTENT_DB_URL environment variable");
      System.err.println(
          "  3) --confirm-db flag to explicitly confirm writing to the configured database.");
      return null;
    }

    return props;
  }

  public static SpringApplicationBuilder createCliApplicationBuilder(
      Class<?> sourceClass, Map<String, Object> extraProps) {
    Map<String, Object> cliProps = new HashMap<>();
    cliProps.put("spring.main.web-application-type", "none");
    cliProps.put("server.port", "-1");
    if (extraProps != null) {
      cliProps.putAll(extraProps);
    }

    return new SpringApplicationBuilder(sourceClass)
        .web(WebApplicationType.NONE)
        .bannerMode(Banner.Mode.OFF)
        .profiles("cli")
        .properties(cliProps)
        .initializers(
            ctx -> {
              ctx.getEnvironment()
                  .getPropertySources()
                  .addFirst(
                      new org.springframework.core.env.MapPropertySource(
                          "explicitCliDbProps", cliProps));
            });
  }

  private static ConfigurableApplicationContext createSpringCliContext(
      Map<String, Object> extraProps) {
    java.util.List<String> runArgs = new java.util.ArrayList<>();
    runArgs.add("content");
    if (extraProps != null) {
      for (Map.Entry<String, Object> entry : extraProps.entrySet()) {
        if (entry.getValue() != null) {
          runArgs.add("--" + entry.getKey() + "=" + entry.getValue());
        }
      }
    }

    return createCliApplicationBuilder(EnglishLearningApplication.class, extraProps)
        .run(runArgs.toArray(new String[0]));
  }

  private static void printUsage() {
    System.out.println("Usage: content <command> [arguments] [options]");
    System.out.println("Commands:");
    System.out.println(
        "  validate <packageDir>                     Validate schemas, references, paths, markdown files");
    System.out.println(
        "  dry-run <packageDir>                      Validate and report counts, checksum, and unverified warnings");
    System.out.println(
        "  import <packageDir> [--confirm-db]        Import package in DRAFT status in a single transaction");
    System.out.println(
        "  publish <key> <version> [--confirm-db]    Publish DRAFT revision making it immutable");
  }
}
