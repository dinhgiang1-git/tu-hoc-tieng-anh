package vn.englishlearning.identity.application;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.identity.domain.AppUser;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.domain.UserRole;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;

/**
 * Bootstraps the initial administrator user if the user table is empty. Reads credentials strictly
 * from environment variables without hardcoded secrets.
 */
@Component
public class BootstrapUserRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapUserRunner.class);

  private final AppUserRepository userRepository;
  private final LearnerPreferencesRepository preferencesRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Value("${BOOTSTRAP_ADMIN_USERNAME:admin}")
  private String bootstrapUsername;

  @Value("${BOOTSTRAP_ADMIN_PASSWORD:#{null}}")
  private String bootstrapPassword;

  public BootstrapUserRunner(
      AppUserRepository userRepository,
      LearnerPreferencesRepository preferencesRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.userRepository = userRepository;
    this.preferencesRepository = preferencesRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (args.length > 0 && "content".equalsIgnoreCase(args[0])) {
      log.debug("Skipping BootstrapUserRunner in CLI mode.");
      return;
    }

    if (userRepository.count() > 0) {
      return;
    }

    if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
      log.warn(
          "No users exist and BOOTSTRAP_ADMIN_PASSWORD is missing. Login is unavailable. "
              + "Run scripts/initialize-admin.ps1 from the project root, or set "
              + "BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD in backend/.env, "
              + "then restart the backend from the backend directory.");
      return;
    }

    Instant now = Instant.now(clock);
    AppUser admin =
        new AppUser(
            bootstrapUsername,
            passwordEncoder.encode(bootstrapPassword),
            "System Administrator",
            bootstrapUsername + "@englishlearning.vn",
            UserRole.ADMIN,
            now);

    AppUser savedAdmin = userRepository.save(admin);
    preferencesRepository.save(
        new LearnerPreferences(savedAdmin.getId(), 30, "MON,WED,FRI", "Asia/Ho_Chi_Minh", now));

    log.info("Successfully bootstrapped initial admin user: {}", bootstrapUsername);
  }
}
