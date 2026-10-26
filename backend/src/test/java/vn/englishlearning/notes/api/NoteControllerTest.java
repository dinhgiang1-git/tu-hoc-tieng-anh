package vn.englishlearning.notes.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.englishlearning.notes.application.NoteService;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.api.GlobalExceptionHandler;
import vn.englishlearning.shared.domain.DomainException;

class NoteControllerTest {

  private NoteService noteService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UUID noteId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();
  private final UUID lessonId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-10-26T10:00:00Z");

  @BeforeEach
  void setUp() {
    noteService = mock(NoteService.class);
    NoteController controller = new NoteController(noteService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("GET /api/v1/me/notes returns 200 with paged response")
  void getMyNotes_returnsOk() throws Exception {
    NoteResponse item =
        new NoteResponse(noteId, enrollmentId, lessonId, null, "Note content", 1L, now, now);
    NotePageResponse pageResponse = new NotePageResponse(List.of(item), 0, 20, 1, 1);

    when(noteService.getNotes(eq(enrollmentId), eq(lessonId), eq(0), eq(20)))
        .thenReturn(pageResponse);

    mockMvc
        .perform(
            get("/api/v1/me/notes")
                .param("enrollmentId", enrollmentId.toString())
                .param("lessonId", lessonId.toString())
                .param("page", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(noteId.toString()))
        .andExpect(jsonPath("$.items[0].content").value("Note content"))
        .andExpect(jsonPath("$.totalItems").value(1));
  }

  @Test
  @DisplayName("POST /api/v1/notes returns 201 Created on success")
  void createNote_returnsCreated() throws Exception {
    CreateNoteRequest request = new CreateNoteRequest(enrollmentId, lessonId, null, "Hello note");
    NoteResponse response =
        new NoteResponse(noteId, enrollmentId, lessonId, null, "Hello note", 0L, now, now);

    when(noteService.createNote(any(CreateNoteRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(noteId.toString()))
        .andExpect(jsonPath("$.content").value("Hello note"))
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  @DisplayName(
      "POST /api/v1/notes returns 409 Conflict when lesson note already exists with different text")
  void createNote_conflict_returns409() throws Exception {
    CreateNoteRequest request =
        new CreateNoteRequest(enrollmentId, lessonId, null, "Conflict note");
    when(noteService.createNote(any(CreateNoteRequest.class)))
        .thenThrow(new DomainException(ErrorCode.STATE_CONFLICT, "Ghi chú đã tồn tại."));

    mockMvc
        .perform(
            post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
  }

  @Test
  @DisplayName("PATCH /api/v1/notes/{id} returns 200 with updated note")
  void updateNote_returnsOk() throws Exception {
    UpdateNoteRequest request = new UpdateNoteRequest("Updated content", 1L);
    NoteResponse response =
        new NoteResponse(noteId, enrollmentId, lessonId, null, "Updated content", 2L, now, now);

    when(noteService.updateNote(eq(noteId), any(UpdateNoteRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/v1/notes/{id}", noteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Updated content"))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  @DisplayName("PATCH /api/v1/notes/{id} returns 409 VERSION_CONFLICT on stale version")
  void updateNote_staleVersion_returns409() throws Exception {
    UpdateNoteRequest request = new UpdateNoteRequest("Updated content", 1L);
    when(noteService.updateNote(eq(noteId), any(UpdateNoteRequest.class)))
        .thenThrow(new DomainException(ErrorCode.VERSION_CONFLICT, "Phiên bản cũ."));

    mockMvc
        .perform(
            patch("/api/v1/notes/{id}", noteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  }

  @Test
  @DisplayName("DELETE /api/v1/notes/{id} returns 204 No Content on success")
  void deleteNote_returnsNoContent() throws Exception {
    doNothing().when(noteService).deleteNote(noteId, 2L);

    mockMvc
        .perform(delete("/api/v1/notes/{id}", noteId).param("expectedVersion", "2"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /api/v1/notes/{id} returns 409 Conflict when expectedVersion is stale")
  void deleteNote_staleVersion_returns409() throws Exception {
    doThrow(new DomainException(ErrorCode.VERSION_CONFLICT, "Phiên bản cũ."))
        .when(noteService)
        .deleteNote(noteId, 1L);

    mockMvc
        .perform(delete("/api/v1/notes/{id}", noteId).param("expectedVersion", "1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
  }
}
