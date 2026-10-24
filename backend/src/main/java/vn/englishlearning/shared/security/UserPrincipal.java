package vn.englishlearning.shared.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.englishlearning.identity.domain.UserRole;

/** Custom UserDetails implementation holding the strongly typed UUID and UserRole. */
public class UserPrincipal implements UserDetails, Serializable {

  private final UUID userId;
  private final String username;
  private final String password;
  private final UserRole role;
  private final boolean active;

  public UserPrincipal(
      UUID userId, String username, String password, UserRole role, boolean active) {
    this.userId = userId;
    this.username = username;
    this.password = password;
    this.role = role;
    this.active = active;
  }

  public UUID getUserId() {
    return userId;
  }

  public UserRole getRole() {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
