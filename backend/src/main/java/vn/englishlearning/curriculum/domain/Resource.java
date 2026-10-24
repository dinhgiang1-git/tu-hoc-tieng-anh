package vn.englishlearning.curriculum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "resource")
public class Resource {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "revision_id", nullable = false)
  private CurriculumRevision revision;

  @Column(name = "resource_key", nullable = false, length = 100)
  private String resourceKey;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "url", nullable = false, columnDefinition = "TEXT")
  private String url;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 20)
  private MediaType mediaType;

  @Column(name = "target_locale", nullable = false, length = 10)
  private String targetLocale;

  @Column(name = "attribution", columnDefinition = "TEXT")
  private String attribution;

  @Column(name = "license", columnDefinition = "TEXT")
  private String license;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 30)
  private VerificationStatus verificationStatus;

  protected Resource() {}

  public Resource(
      CurriculumRevision revision,
      String resourceKey,
      String title,
      String url,
      MediaType mediaType,
      String targetLocale,
      String attribution,
      String license,
      VerificationStatus verificationStatus) {
    this.revision = Objects.requireNonNull(revision, "revision must not be null");
    this.resourceKey = Objects.requireNonNull(resourceKey, "resourceKey must not be null");
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.url = Objects.requireNonNull(url, "url must not be null");
    this.mediaType = Objects.requireNonNull(mediaType, "mediaType must not be null");
    this.targetLocale = Objects.requireNonNull(targetLocale, "targetLocale must not be null");
    this.attribution = attribution;
    this.license = license;
    this.verificationStatus =
        Objects.requireNonNull(verificationStatus, "verificationStatus must not be null");
  }

  public UUID getId() {
    return id;
  }

  public CurriculumRevision getRevision() {
    return revision;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public String getTargetLocale() {
    return targetLocale;
  }

  public String getAttribution() {
    return attribution;
  }

  public String getLicense() {
    return license;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }
}
