package vn.englishlearning.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemFactoryTest {

  @Test
  @DisplayName("Should create ProblemDetail with all required RFC 7807 properties")
  void shouldCreateProblemDetailWithRequiredProperties() {
    List<FieldViolation> violations =
        List.of(new FieldViolation("username", "NOT_BLANK", "Username cannot be blank"));

    ProblemDetail problem =
        ProblemFactory.create(
            ErrorCode.VALIDATION_FAILED, "Validation errors occurred", "/api/v1/test", violations);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Bad Request");
    assertThat(problem.getDetail()).isEqualTo("Validation errors occurred");
    assertThat(problem.getInstance().getPath()).isEqualTo("/api/v1/test");

    assertThat(problem.getProperties()).isNotNull();
    assertThat(problem.getProperties().get("code")).isEqualTo("VALIDATION_FAILED");
    assertThat(problem.getProperties().get("traceId")).isNotNull();
    assertThat(problem.getProperties().get("fieldErrors")).isEqualTo(violations);
  }

  @Test
  @DisplayName("Should populate default empty fieldErrors when null provided")
  void shouldPopulateEmptyFieldErrorsWhenNullProvided() {
    ProblemDetail problem = ProblemFactory.create(ErrorCode.RESOURCE_NOT_FOUND, "/api/v1/missing");

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getProperties().get("code")).isEqualTo("RESOURCE_NOT_FOUND");
    assertThat(problem.getProperties().get("fieldErrors")).asList().isEmpty();
  }
}
