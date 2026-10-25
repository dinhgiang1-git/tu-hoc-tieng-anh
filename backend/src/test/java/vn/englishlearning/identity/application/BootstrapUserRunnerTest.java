package vn.englishlearning.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;

class BootstrapUserRunnerTest {
  private final AppUserRepository users = mock(AppUserRepository.class);
  private final LearnerPreferencesRepository preferences = mock(LearnerPreferencesRepository.class);
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final Instant now = Instant.parse("2026-10-25T00:00:00Z");
  private BootstrapUserRunner runner;

  @BeforeEach
  void setUp() {
    runner = new BootstrapUserRunner(users, preferences, encoder, Clock.fixed(now, ZoneOffset.UTC));
    ReflectionTestUtils.setField(runner, "bootstrapUsername", "admin");
    ReflectionTestUtils.setField(runner, "bootstrapPassword", "test-only-password");
  }

  @Test
  void emptyDatabaseCreatesEncodedAdminAndPreferences() {
    UUID userId = UUID.randomUUID();
    when(users.save(any(AppUser.class)))
        .thenAnswer(
            invocation -> {
              AppUser user = invocation.getArgument(0);
              ReflectionTestUtils.setField(user, "id", userId);
              return user;
            });

    runner.run();

    ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
    verify(users).save(userCaptor.capture());
    AppUser admin = userCaptor.getValue();
    assertThat(admin.getUsername()).isEqualTo("admin");
    assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(admin.isActive()).isTrue();
    assertThat(encoder.matches("test-only-password", admin.getPasswordHash())).isTrue();
    assertThat(admin.getPasswordHash()).isNotEqualTo("test-only-password");
    assertThat(admin.getCreatedAt()).isEqualTo(now);
    ArgumentCaptor<LearnerPreferences> preferencesCaptor =
        ArgumentCaptor.forClass(LearnerPreferences.class);
    verify(preferences).save(preferencesCaptor.capture());
    assertThat(preferencesCaptor.getValue().getUserId()).isEqualTo(userId);
    assertThat(preferencesCaptor.getValue().getDailyGoalMinutes()).isEqualTo(30);
    assertThat(preferencesCaptor.getValue().getStudyDays()).isEqualTo("MON,WED,FRI");
    assertThat(preferencesCaptor.getValue().getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void missingPasswordDoesNotCreateUser(String password) {
    ReflectionTestUtils.setField(runner, "bootstrapPassword", password);
    runner.run();
    verify(users).count();
    org.mockito.Mockito.verifyNoMoreInteractions(users);
    verifyNoInteractions(preferences);
  }

  @Test
  void existingUsersAreNeverOverwritten() {
    when(users.count()).thenReturn(1L);
    runner.run();
    verify(users).count();
    org.mockito.Mockito.verifyNoMoreInteractions(users);
    verifyNoInteractions(preferences);
  }

  @Test
  void contentCliDoesNotBootstrap() {
    runner.run("content", "import");
    verifyNoInteractions(users, preferences);
  }
}
