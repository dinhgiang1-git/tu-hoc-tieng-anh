package vn.englishlearning.curriculum.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.Curriculum;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {

  Optional<Curriculum> findByCurriculumKey(String curriculumKey);
}
