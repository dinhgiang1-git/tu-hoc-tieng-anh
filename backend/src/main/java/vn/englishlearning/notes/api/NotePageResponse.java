package vn.englishlearning.notes.api;

import java.util.List;

public record NotePageResponse(
    List<NoteResponse> items, int page, int size, long totalItems, int totalPages) {}
