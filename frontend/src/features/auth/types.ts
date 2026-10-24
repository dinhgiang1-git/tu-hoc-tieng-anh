export type UserRole = "LEARNER" | "ADMIN";

/**
 * User entity model adhering to openapi.yaml UserResponse.
 */
export interface User {
  id: string;
  username: string;
  fullName: string;
  timeZone: string;
  displayName?: string;
  timezone?: string;
  role: UserRole;
}

export interface LearnerPreferences {
  dailyTargetMinutes: number;
  focusedEnrollmentId: string | null;
}
