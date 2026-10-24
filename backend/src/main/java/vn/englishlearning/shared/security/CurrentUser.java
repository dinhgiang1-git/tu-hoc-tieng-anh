package vn.englishlearning.shared.security;

import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;

/**
 * Adapter component for resolving the authenticated user principal. Adheres to
 * BE_CODE_CONVENTION.md (Section 5.1).
 */
@Component
public class CurrentUser {

  public UUID requireUserId() {
    return requirePrincipal().getUserId();
  }

  public UserPrincipal requirePrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new DomainException(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    if (authentication.getPrincipal() instanceof UserPrincipal principal) {
      return principal;
    }

    throw new DomainException(
        ErrorCode.AUTHENTICATION_REQUIRED, "Không tìm thấy thông tin phiên người dùng hợp lệ.");
  }
}
