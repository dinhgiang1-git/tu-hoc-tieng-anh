package vn.englishlearning.shared.api;

/**
 * Standard representation of a single field violation in RFC 7807 problem details. Adheres to
 * BE_CODE_CONVENTION.md (Section 4.2).
 */
public record FieldViolation(String field, String code, String message) {}
