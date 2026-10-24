package vn.englishlearning.contentops.domain;

public class ContentConflictException extends RuntimeException {

  public ContentConflictException(String message) {
    super(message);
  }
}
