package vn.englishlearning.contentops.domain;

public record ValidationError(String file, String path, String message) {

  @Override
  public String toString() {
    return String.format("[%s] %s: %s", file, path, message);
  }
}
