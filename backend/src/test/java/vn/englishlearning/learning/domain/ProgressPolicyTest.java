package vn.englishlearning.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProgressPolicyTest {

  @Test
  @DisplayName("isEffectivelyRequired is true only when both lesson and activity are required")
  void isEffectivelyRequired_truthTable() {
    assertThat(ProgressPolicy.isEffectivelyRequired(true, true)).isTrue();
    assertThat(ProgressPolicy.isEffectivelyRequired(true, false)).isFalse();
    assertThat(ProgressPolicy.isEffectivelyRequired(false, true)).isFalse();
    assertThat(ProgressPolicy.isEffectivelyRequired(false, false)).isFalse();
  }

  @Test
  @DisplayName("percentOrNull returns null when total is zero")
  void percentOrNull_zeroTotal_returnsNull() {
    assertThat(ProgressPolicy.percentOrNull(0, 0)).isNull();
  }

  @Test
  @DisplayName("percentOrNull computes rounded percentage correctly")
  void percentOrNull_validCounts_computesPercentage() {
    assertThat(ProgressPolicy.percentOrNull(19, 19)).isEqualByComparingTo(new BigDecimal("100.0"));
    assertThat(ProgressPolicy.percentOrNull(10, 19)).isEqualByComparingTo(new BigDecimal("52.6"));
    assertThat(ProgressPolicy.percentOrNull(0, 19)).isEqualByComparingTo(new BigDecimal("0.0"));
  }

  @Test
  @DisplayName("percentOrNull throws on invalid inputs")
  void percentOrNull_invalidCounts_throwsException() {
    assertThatThrownBy(() -> ProgressPolicy.percentOrNull(-1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProgressPolicy.percentOrNull(11, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProgressPolicy.percentOrNull(5, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
