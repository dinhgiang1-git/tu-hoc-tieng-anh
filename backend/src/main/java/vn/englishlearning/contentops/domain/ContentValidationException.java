package vn.englishlearning.contentops.domain;

import java.util.List;

public class ContentValidationException extends RuntimeException {

  private final List<ValidationError> errors;

  public ContentValidationException(String message, List<ValidationError> errors) {
    super(message);
    this.errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}
