package vn.englishlearning.contentops.domain;

public record ValidationWarning(String file, String path, String message) {

  @Override
  public String toString() {
    return String.format("[%s] %s: %s", file, path, message);
  }
}
