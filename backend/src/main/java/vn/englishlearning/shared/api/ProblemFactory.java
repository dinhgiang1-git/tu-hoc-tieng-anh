package vn.englishlearning.shared.api;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;

/**
 * Factory for creating standardized RFC 7807 ProblemDetail objects. Guarantees consistent 'code',
 * 'traceId', and 'fieldErrors' properties.
 */
public final class ProblemFactory {

  private ProblemFactory() {}

  public static ProblemDetail create(
      ErrorCode errorCode, String detail, String instancePath, List<FieldViolation> fieldErrors) {
    ProblemDetail problem = ProblemDetail.forStatus(errorCode.getStatus());
    problem.setType(URI.create("about:blank"));
    problem.setTitle(errorCode.getStatus().getReasonPhrase());
    problem.setDetail(detail != null ? detail : errorCode.getDefaultDetail());
    if (instancePath != null) {
      problem.setInstance(URI.create(instancePath));
    }
    problem.setProperty("code", errorCode.name());
    problem.setProperty("traceId", UUID.randomUUID().toString());
    problem.setProperty("fieldErrors", fieldErrors != null ? fieldErrors : Collections.emptyList());
    return problem;
  }

  public static ProblemDetail create(ErrorCode errorCode, String detail, String instancePath) {
    return create(errorCode, detail, instancePath, Collections.emptyList());
  }

  public static ProblemDetail create(ErrorCode errorCode, String instancePath) {
    return create(errorCode, errorCode.getDefaultDetail(), instancePath, Collections.emptyList());
  }
}
