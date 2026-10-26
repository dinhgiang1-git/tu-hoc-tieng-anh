package vn.englishlearning.notes.api;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateNoteRequest(
    UUID enrollmentId,
    UUID lessonId,
    UUID activityId,
    @Size(max = 10000, message = "Nội dung ghi chú tối đa 10,000 ký tự.") String content) {}
