package vn.englishlearning.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core User entity for identity management. Adheres to BE_CODE_CONVENTION.md (Section 3: class with
 * protected constructor, explicit fields).
 */
@Entity
@Table(name = "app_user")
public class AppUser {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "email", length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private UserRole role = UserRole.LEARNER;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AppUser() {
    // JPA required constructor
  }

  public AppUser(
      String username,
      String passwordHash,
      String displayName,
      String email,
      UserRole role,
      Instant now) {
    this.username = Objects.requireNonNull(username, "username must not be null");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    this.displayName = displayName;
    this.email = email;
    this.role = role != null ? role : UserRole.LEARNER;
    this.isActive = true;
    this.createdAt = Objects.requireNonNull(now, "createdAt must not be null");
    this.updatedAt = now;
  }

  public AppUser(
      String username, String passwordHash, String displayName, String email, Instant now) {
    this(username, passwordHash, displayName, email, UserRole.LEARNER, now);
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash, Instant now) {
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName, Instant now) {
    this.displayName = displayName;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email, Instant now) {
    this.email = email;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role, Instant now) {
    this.role = Objects.requireNonNull(role, "role must not be null");
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active, Instant now) {
    this.isActive = active;
    this.updatedAt = Objects.requireNonNull(now, "updatedAt must not be null");
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
