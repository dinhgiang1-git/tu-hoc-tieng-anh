package vn.englishlearning.learning.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain policy calculating progress metrics and effective requirements according to
 * BE_CODE_CONVENTION.md.
 */
public final class ProgressPolicy {

  private ProgressPolicy() {}

  public static boolean isEffectivelyRequired(boolean lessonRequired, boolean activityRequired) {
    return lessonRequired && activityRequired;
  }

  public static BigDecimal percentOrNull(long completed, long total) {
    if (total < 0 || completed < 0 || completed > total) {
      throw new IllegalArgumentException("Invalid progress counts");
    }
    if (total == 0) {
      return null;
    }
    return BigDecimal.valueOf(completed)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
  }
}
