package vn.englishlearning.contentops.cli;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Ensures explicit CLI database target properties (URL, username, password) are elevated to the
 * highest precedence PropertySource, preventing them from being overridden by application-dev.yml
 * or other profile config files.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class CliEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String[] DB_TARGET_KEYS = {
    "spring.datasource.url",
    "spring.datasource.username",
    "spring.datasource.password",
    "spring.datasource.driver-class-name"
  };

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    PropertySource<?> defaultProps = environment.getPropertySources().get("defaultProperties");
    if (defaultProps != null) {
      Map<String, Object> overrides = new HashMap<>();
      for (String key : DB_TARGET_KEYS) {
        if (defaultProps.containsProperty(key)) {
          overrides.put(key, defaultProps.getProperty(key));
        }
      }
      if (!overrides.isEmpty()) {
        environment
            .getPropertySources()
            .addFirst(new MapPropertySource("explicitCliOverrideProperties", overrides));
      }
    }
  }
}
