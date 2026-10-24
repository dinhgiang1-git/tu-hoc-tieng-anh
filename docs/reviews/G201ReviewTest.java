package vn.englishlearning.contentops.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import vn.englishlearning.curriculum.domain.Activity;
import vn.englishlearning.curriculum.infrastructure.ActivityRepository;

class G201ReviewTest extends ImportAndPublishServiceTest {
  @Test
  void reviewPilotActivitiesHaveSequentialOrder() throws Exception {
    importPackage_newCurriculumAndRevision_success();
    Field field = ImportAndPublishServiceTest.class.getDeclaredField("activityRepository");
    field.setAccessible(true);
    ActivityRepository repository = (ActivityRepository) field.get(this);
    ArgumentCaptor<Activity> activities = ArgumentCaptor.forClass(Activity.class);
    verify(repository, times(21)).save(activities.capture());
    assertThat(activities.getAllValues().subList(0, 4))
        .extracting(Activity::getOrderIndex).containsExactly(1, 2, 3, 4);
  }

  @Configuration(proxyBeanMethods = false)
  static class NoDatabaseBeans {}

  @Test
  void reviewExplicitCliDatabaseOverridesApplicationConfig() {
    String expected = "jdbc:postgresql://127.0.0.1:1/review_no_database_connection";
    AtomicReference<String> actual = new AtomicReference<>();
    assertThatThrownBy(() -> new SpringApplicationBuilder(NoDatabaseBeans.class)
        .web(WebApplicationType.NONE)
        .properties(Map.of("spring.datasource.url", expected))
        .initializers(ctx -> {
          actual.set(ctx.getEnvironment().getProperty("spring.datasource.url"));
          throw new IllegalStateException("Review stopped before bean creation or any DB connection");
        }).run("content"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(actual.get()).as("Explicit CLI DB target must win; no DB was opened").isEqualTo(expected);
  }

  @Test
  void reviewRootResourcesSymlinkMustBeRejected(@TempDir Path dir) throws Exception {
    Path source = Paths.get("../content/pilot-curriculum/v1").toRealPath();
    Path pkg = dir.resolve("package");
    Files.createDirectories(pkg);
    try (var paths = Files.walk(source)) {
      for (Path file : paths.toList()) {
        Path target = pkg.resolve(source.relativize(file));
        if (Files.isDirectory(file)) Files.createDirectories(target);
        else Files.copy(file, target);
      }
    }
    Path outside = dir.resolve("outside-resources.json");
    Files.copy(pkg.resolve("resources.json"), outside);
    Files.delete(pkg.resolve("resources.json"));
    Files.createSymbolicLink(pkg.resolve("resources.json"), outside);
    var validator = new ContentPackageValidator(new com.fasterxml.jackson.databind.ObjectMapper());
    assertThat(validator.validate(pkg).hasErrors()).as("Root JSON must stay inside package").isTrue();
  }
}
