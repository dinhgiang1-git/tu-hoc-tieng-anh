package vn.englishlearning.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationClockConfigTest {

  private final ApplicationClockConfig config = new ApplicationClockConfig();

  @Test
  @DisplayName("Should create UTC clock bean")
  void shouldCreateUtcClockBean() {
    Clock clock = config.clock();

    assertThat(clock).isNotNull();
    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
  }
}
