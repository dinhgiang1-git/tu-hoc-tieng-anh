package vn.englishlearning.curriculum.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.api.CatalogResponses.*;

/** Read projections intentionally omit lesson Markdown and activity payloads. */
@Repository
public class CurriculumCatalogRepository {
  private final JdbcTemplate jdbc;

  public CurriculumCatalogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record RevisionMetadata(
      UUID curriculumId,
      String slug,
      String locale,
      UUID revisionId,
      String version,
      String status,
      String title,
      String description,
      String authors,
      String provenance) {}

  public record PhaseRow(UUID id, String key, String title, int orderIndex) {}

  public record WeekRow(
      UUID phaseId, UUID id, String key, String title, int weekNumber, int orderIndex) {}

  public record LessonRow(UUID weekId, LessonOutlineDto lesson) {}

  private static final String METADATA =
      """
      SELECT c.id AS curriculum_id, c.curriculum_key, c.target_locale,
             r.id, r.version, r.status, r.title, r.description, r.authors, r.provenance
      FROM curriculum c JOIN curriculum_revision r ON r.curriculum_id = c.id
      """;

  public List<CurriculumSummaryResponse> listPublished() {
    return jdbc.query(
        """
        SELECT * FROM (
          SELECT DISTINCT ON (c.id) c.id, c.curriculum_key, c.target_locale,
                 r.id AS revision_id, r.version, r.title, r.description
          FROM curriculum c JOIN curriculum_revision r ON r.curriculum_id = c.id
          WHERE r.status = 'PUBLISHED'
          ORDER BY c.id, r.published_at DESC, r.created_at DESC, r.id DESC
        ) catalog ORDER BY curriculum_key
        """,
        (rs, row) ->
            new CurriculumSummaryResponse(
                rs.getObject("id", UUID.class),
                rs.getString("curriculum_key"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("target_locale"),
                rs.getObject("revision_id", UUID.class),
                rs.getString("version")));
  }

  public Optional<RevisionMetadata> findPublished(String slug) {
    return jdbc
        .query(
            METADATA
                + """
        WHERE c.curriculum_key = ? AND r.status = 'PUBLISHED'
        ORDER BY r.published_at DESC, r.created_at DESC, r.id DESC LIMIT 1
        """,
            (rs, row) -> metadata(rs),
            slug)
        .stream()
        .findFirst();
  }

  public Optional<RevisionMetadata> findVisibleRevision(UUID id) {
    return jdbc
        .query(
            METADATA + "WHERE r.id = ? AND r.status = 'PUBLISHED'", (rs, row) -> metadata(rs), id)
        .stream()
        .findFirst();
  }

  public List<PhaseRow> phases(UUID revisionId) {
    return jdbc.query(
        "SELECT id, phase_key, title, order_index FROM curriculum_phase WHERE revision_id = ? ORDER BY order_index, id",
        (rs, row) ->
            new PhaseRow(
                rs.getObject("id", UUID.class),
                rs.getString("phase_key"),
                rs.getString("title"),
                rs.getInt("order_index")),
        revisionId);
  }

  public List<WeekRow> weeks(UUID revisionId) {
    return jdbc.query(
        """
        SELECT w.id, w.phase_id, w.week_key, w.title, w.week_number, w.order_index
        FROM curriculum_week w JOIN curriculum_phase p ON p.id = w.phase_id
        WHERE p.revision_id = ? ORDER BY p.order_index, w.order_index, w.id
        """,
        (rs, row) ->
            new WeekRow(
                rs.getObject("phase_id", UUID.class),
                rs.getObject("id", UUID.class),
                rs.getString("week_key"),
                rs.getString("title"),
                rs.getInt("week_number"),
                rs.getInt("order_index")),
        revisionId);
  }

  public List<LessonRow> lessons(UUID revisionId) {
    return jdbc.query(
        """
        SELECT l.id, l.week_id, l.lesson_key, l.title, l.order_index, l.estimated_minutes, l.is_required
        FROM lesson l JOIN curriculum_week w ON w.id = l.week_id
        JOIN curriculum_phase p ON p.id = w.phase_id
        WHERE p.revision_id = ? ORDER BY p.order_index, w.order_index, l.order_index, l.id
        """,
        (rs, row) ->
            new LessonRow(
                rs.getObject("week_id", UUID.class),
                new LessonOutlineDto(
                    rs.getObject("id", UUID.class),
                    rs.getString("lesson_key"),
                    rs.getString("title"),
                    rs.getInt("order_index"),
                    rs.getInt("estimated_minutes"),
                    rs.getBoolean("is_required"))),
        revisionId);
  }

  private static RevisionMetadata metadata(ResultSet rs) throws SQLException {
    return new RevisionMetadata(
        rs.getObject("curriculum_id", UUID.class),
        rs.getString("curriculum_key"),
        rs.getString("target_locale"),
        rs.getObject("id", UUID.class),
        rs.getString("version"),
        rs.getString("status"),
        rs.getString("title"),
        rs.getString("description"),
        rs.getString("authors"),
        rs.getString("provenance"));
  }
}
