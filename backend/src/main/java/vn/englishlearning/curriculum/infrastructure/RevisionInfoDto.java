package vn.englishlearning.curriculum.infrastructure;

import java.util.UUID;

public record RevisionInfoDto(UUID id, String title, String version) {

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getVersion() {
    return version;
  }
}
