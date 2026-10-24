package vn.englishlearning.identity.api;

import java.util.UUID;

/** Learner preferences response adhering to openapi.yaml LearnerPreferencesResponse. */
public record LearnerPreferencesResponse(int dailyTargetMinutes, UUID focusedEnrollmentId) {}
