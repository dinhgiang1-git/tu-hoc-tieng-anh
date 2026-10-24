package vn.englishlearning.curriculum.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.curriculum.domain.Resource;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

  List<Resource> findByRevisionId(UUID revisionId);

  Optional<Resource> findByRevisionIdAndResourceKey(UUID revisionId, String resourceKey);
}
