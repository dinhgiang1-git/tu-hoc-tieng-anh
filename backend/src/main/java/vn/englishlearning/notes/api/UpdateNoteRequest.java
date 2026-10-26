package vn.englishlearning.notes.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
    @Size(max = 10000, message = "Nội dung ghi chú tối đa 10,000 ký tự.") String content,
    @NotNull(message = "expectedVersion không được để trống.")
        @Min(value = 0, message = "expectedVersion phải không âm.")
        Long expectedVersion) {}
