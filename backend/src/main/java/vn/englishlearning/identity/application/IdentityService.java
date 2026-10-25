package vn.englishlearning.identity.application;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.identity.api.LearnerPreferencesResponse;
import vn.englishlearning.identity.api.LoginRequest;
import vn.englishlearning.identity.api.UpdatePreferencesRequest;
import vn.englishlearning.identity.api.UserResponse;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.security.CurrentUser;
import vn.englishlearning.shared.security.UserPrincipal;

/** Application service managing authentication, session lifecycle, and learner preferences. */
@Service
public class IdentityService {

  private final AppUserRepository userRepository;
  private final LearnerPreferencesRepository preferencesRepository;
  private final vn.englishlearning.enrollment.infrastructure.EnrollmentRepository
      enrollmentRepository;
  private final PasswordEncoder passwordEncoder;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
  private final SecurityContextRepository securityContextRepository;

  public IdentityService(
      AppUserRepository userRepository,
      LearnerPreferencesRepository preferencesRepository,
      vn.englishlearning.enrollment.infrastructure.EnrollmentRepository enrollmentRepository,
      PasswordEncoder passwordEncoder,
      CurrentUser currentUser,
      Clock clock,
      SessionAuthenticationStrategy sessionAuthenticationStrategy,
      SecurityContextRepository securityContextRepository) {
    this.userRepository = userRepository;
    this.preferencesRepository = preferencesRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.passwordEncoder = passwordEncoder;
    this.currentUser = currentUser;
    this.clock = clock;
    this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    this.securityContextRepository = securityContextRepository;
  }

  @Transactional
  public UserResponse login(
      LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    AppUser user =
        userRepository
            .findByUsername(request.username())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new DomainException(ErrorCode.INVALID_CREDENTIALS));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
    }

    UserPrincipal principal =
        new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getPasswordHash(),
            user.getRole(),
            user.isActive());

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    // Execute standard session fixation protection & CSRF token rotation
    sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);

    LearnerPreferences preferences =
        preferencesRepository
            .findById(user.getId())
            .orElseGet(
                () ->
                    preferencesRepository.save(
                        new LearnerPreferences(
                            user.getId(),
                            30,
                            "MON,WED,FRI",
                            "Asia/Ho_Chi_Minh",
                            Instant.now(clock))));

    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
        preferences.getTimezone(),
        user.getRole());
  }

  public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
    securityContextRepository.saveContext(emptyContext, httpRequest, httpResponse);
  }

  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    UUID userId = currentUser.requireUserId();
    AppUser user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new DomainException(ErrorCode.AUTHENTICATION_REQUIRED));

    LearnerPreferences preferences =
        preferencesRepository
            .findById(userId)
            .orElseGet(
                () ->
                    new LearnerPreferences(
                        userId, 30, "MON,WED,FRI", "Asia/Ho_Chi_Minh", Instant.now(clock)));

    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
        preferences.getTimezone(),
        user.getRole());
  }

  @Transactional(readOnly = true)
  public LearnerPreferencesResponse getPreferences() {
    UUID userId = currentUser.requireUserId();
    LearnerPreferences preferences =
        preferencesRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tùy chọn học tập."));

    return new LearnerPreferencesResponse(
        preferences.getDailyGoalMinutes(), preferences.getFocusedEnrollmentId());
  }

  @Transactional
  public LearnerPreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
    UUID userId = currentUser.requireUserId();
    Instant now = Instant.now(clock);

    LearnerPreferences preferences =
        preferencesRepository
            .findById(userId)
            .orElseGet(
                () ->
                    preferencesRepository.save(
                        new LearnerPreferences(
                            userId, 30, "MON,WED,FRI", "Asia/Ho_Chi_Minh", now)));

    if (request.isDailyTargetMinutesPresent() && request.dailyTargetMinutes() != null) {
      preferences.setDailyGoalMinutes(request.dailyTargetMinutes());
    }
    if (request.isFocusedEnrollmentIdPresent()) {
      if (request.focusedEnrollmentId() != null) {
        if (!enrollmentRepository.existsByIdAndUserId(request.focusedEnrollmentId(), userId)) {
          throw new DomainException(
              ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lần đăng ký học tập.");
        }
      }
      preferences.setFocusedEnrollmentId(request.focusedEnrollmentId(), now);
    }
    preferencesRepository.save(preferences);

    return new LearnerPreferencesResponse(
        preferences.getDailyGoalMinutes(), preferences.getFocusedEnrollmentId());
  }
}
