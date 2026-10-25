package vn.englishlearning.curriculum.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.CurriculumRevision;

@Repository
public interface CurriculumRevisionRepository extends JpaRepository<CurriculumRevision, UUID> {

  Optional<CurriculumRevision> findByCurriculumIdAndVersion(UUID curriculumId, String version);

  Optional<CurriculumRevision> findByCurriculumCurriculumKeyAndVersion(
      String curriculumKey, String version);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.RevisionInfoDto(
          r.id, r.title, r.version
      )
      FROM CurriculumRevision r
      WHERE r.id = :revisionId
      """)
  Optional<RevisionInfoDto> findRevisionInfoById(
      @org.springframework.data.repository.query.Param("revisionId") UUID revisionId);

  @org.springframework.data.jpa.repository.Query(
      """
      SELECT new vn.englishlearning.curriculum.infrastructure.RevisionInfoDto(
          r.id, r.title, r.version
      )
      FROM CurriculumRevision r
      WHERE r.id IN :revisionIds
      """)
  java.util.List<RevisionInfoDto> findRevisionInfosByIds(
      @org.springframework.data.repository.query.Param("revisionIds")
          java.util.Collection<UUID> revisionIds);
}
