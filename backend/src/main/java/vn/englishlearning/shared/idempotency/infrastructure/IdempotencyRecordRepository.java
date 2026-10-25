package vn.englishlearning.shared.idempotency.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

  Optional<IdempotencyRecord> findByUserIdAndOperationAndIdempotencyKey(
      UUID userId, String operation, String idempotencyKey);
}
