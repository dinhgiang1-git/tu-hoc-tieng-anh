package vn.englishlearning.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ApplicationClock. Services must inject Clock rather than accessing system clock
 * directly, allowing predictable time manipulation during unit and integration tests.
 */
@Configuration
public class ApplicationClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
