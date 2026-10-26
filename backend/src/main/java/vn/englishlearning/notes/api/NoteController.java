package vn.englishlearning.notes.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.englishlearning.notes.application.NoteService;

@RestController
@RequestMapping("/api/v1")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @GetMapping("/me/notes")
  public ResponseEntity<NotePageResponse> getMyNotes(
      @RequestParam(name = "enrollmentId", required = false) UUID enrollmentId,
      @RequestParam(name = "lessonId", required = false) UUID lessonId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    NotePageResponse response = noteService.getNotes(enrollmentId, lessonId, page, size);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/notes")
  public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody CreateNoteRequest request) {
    NoteResponse response = noteService.createNote(request);
    return ResponseEntity.created(URI.create("/api/v1/notes/" + response.id())).body(response);
  }

  @PatchMapping("/notes/{id}")
  public ResponseEntity<NoteResponse> updateNote(
      @PathVariable("id") UUID id, @Valid @RequestBody UpdateNoteRequest request) {
    NoteResponse response = noteService.updateNote(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/notes/{id}")
  public ResponseEntity<Void> deleteNote(
      @PathVariable("id") UUID id, @RequestParam("expectedVersion") Long expectedVersion) {
    noteService.deleteNote(id, expectedVersion);
    return ResponseEntity.noContent().build();
  }
}
