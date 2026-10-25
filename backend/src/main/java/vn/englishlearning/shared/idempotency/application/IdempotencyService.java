package vn.englishlearning.shared.idempotency.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.idempotency.infrastructure.IdempotencyRecordRepository;

@Service
public class IdempotencyService {

  private final IdempotencyRecordRepository repository;
  private final ObjectMapper mapper;
  private final Clock clock;

  public IdempotencyService(
      IdempotencyRecordRepository repository, ObjectMapper mapper, Clock clock) {
    this.repository = repository;
    this.mapper = mapper;
    this.clock = clock;
  }

  public String validateAndNormalizeKey(String rawKey) {
    if (rawKey == null || rawKey.isBlank()) {
      throw new DomainException(ErrorCode.INVALID_REQUEST, "Idempotency-Key header is required.");
    }
    try {
      return UUID.fromString(rawKey.trim()).toString();
    } catch (IllegalArgumentException ex) {
      throw new DomainException(
          ErrorCode.INVALID_REQUEST, "Idempotency-Key header must be a valid UUID.");
    }
  }

  public String hashPayload(String canonicalPayload) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  public Optional<IdempotencyRecord> findExisting(UUID userId, String operation, String key) {
    return repository.findByUserIdAndOperationAndIdempotencyKey(userId, operation, key);
  }

  public <T> T replay(IdempotencyRecord record, String currentHash, Class<T> responseClass) {
    if (!record.getRequestHash().equals(currentHash)) {
      throw new DomainException(
          ErrorCode.IDEMPOTENCY_CONFLICT,
          "Idempotency key has already been used with a different request payload.");
    }
    try {
      return mapper.readValue(record.getResponseBody(), responseClass);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize cached idempotency response", ex);
    }
  }

  public <T> void record(
      UUID userId, String operation, String key, String requestHash, int statusCode, T response) {
    try {
      String json = mapper.writeValueAsString(response);
      IdempotencyRecord record =
          new IdempotencyRecord(
              userId, operation, key, requestHash, statusCode, json, Instant.now(clock));
      repository.save(record);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize idempotency response", ex);
    }
  }
}
