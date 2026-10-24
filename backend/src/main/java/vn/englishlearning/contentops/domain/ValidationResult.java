package vn.englishlearning.contentops.domain;

import java.util.Collections;
import java.util.List;

public record ValidationResult(
    boolean valid, List<ValidationError> errors, List<ValidationWarning> warnings) {

  public ValidationResult {
    errors = errors == null ? List.of() : Collections.unmodifiableList(errors);
    warnings = warnings == null ? List.of() : Collections.unmodifiableList(warnings);
  }

  public static ValidationResult success(List<ValidationWarning> warnings) {
    return new ValidationResult(true, List.of(), warnings);
  }

  public static ValidationResult failure(
      List<ValidationError> errors, List<ValidationWarning> warnings) {
    return new ValidationResult(false, errors, warnings);
  }

  public boolean hasErrors() {
    return !valid || !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}
