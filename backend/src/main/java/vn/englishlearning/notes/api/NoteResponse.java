package vn.englishlearning.notes.api;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
    UUID id,
    UUID enrollmentId,
    UUID lessonId,
    UUID activityId,
    String content,
    Long version,
    Instant createdAt,
    Instant updatedAt) {}
