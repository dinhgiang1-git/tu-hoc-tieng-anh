package vn.englishlearning.shared.idempotency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "operation", nullable = false, length = 64)
  private String operation;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "status_code", nullable = false)
  private int statusCode;

  @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
  private String responseBody;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected IdempotencyRecord() {}

  public IdempotencyRecord(
      UUID userId,
      String operation,
      String idempotencyKey,
      String requestHash,
      int statusCode,
      String responseBody,
      Instant createdAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.operation = Objects.requireNonNull(operation, "operation must not be null");
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    this.requestHash = Objects.requireNonNull(requestHash, "requestHash must not be null");
    this.statusCode = statusCode;
    this.responseBody = Objects.requireNonNull(responseBody, "responseBody must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getOperation() {
    return operation;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
