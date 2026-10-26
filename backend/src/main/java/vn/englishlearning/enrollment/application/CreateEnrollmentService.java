package vn.englishlearning.enrollment.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.englishlearning.curriculum.domain.CurriculumRevision;
import vn.englishlearning.curriculum.domain.Lesson;
import vn.englishlearning.curriculum.domain.RevisionStatus;
import vn.englishlearning.curriculum.infrastructure.CurriculumRevisionRepository;
import vn.englishlearning.curriculum.infrastructure.LessonRepository;
import vn.englishlearning.enrollment.api.EnrollmentRequests.CreateEnrollmentRequest;
import vn.englishlearning.enrollment.api.EnrollmentResponses.EnrollmentResponse;
import vn.englishlearning.enrollment.domain.Enrollment;
import vn.englishlearning.enrollment.domain.EnrollmentStatus;
import vn.englishlearning.enrollment.domain.LessonSchedule;
import vn.englishlearning.enrollment.infrastructure.EnrollmentRepository;
import vn.englishlearning.enrollment.infrastructure.LessonScheduleRepository;
import vn.englishlearning.identity.domain.LearnerPreferences;
import vn.englishlearning.identity.infrastructure.AppUserRepository;
import vn.englishlearning.identity.infrastructure.LearnerPreferencesRepository;
import vn.englishlearning.shared.api.ErrorCode;
import vn.englishlearning.shared.domain.DomainException;
import vn.englishlearning.shared.idempotency.application.IdempotencyService;
import vn.englishlearning.shared.idempotency.domain.IdempotencyRecord;
import vn.englishlearning.shared.security.CurrentUser;

@Service
public class CreateEnrollmentService {

  private final EnrollmentRepository enrollmentRepository;
  private final LessonScheduleRepository lessonScheduleRepository;
  private final CurriculumRevisionRepository revisionRepository;
  private final LessonRepository lessonRepository;
  private final LearnerPreferencesRepository preferencesRepository;
  private final AppUserRepository userRepository;
  private final IdempotencyService idempotencyService;
  private final CurrentUser currentUser;
  private final Clock clock;

  public CreateEnrollmentService(
      EnrollmentRepository enrollmentRepository,
      LessonScheduleRepository lessonScheduleRepository,
      CurriculumRevisionRepository revisionRepository,
      LessonRepository lessonRepository,
      LearnerPreferencesRepository preferencesRepository,
      AppUserRepository userRepository,
      IdempotencyService idempotencyService,
      CurrentUser currentUser,
      Clock clock) {
    this.enrollmentRepository = enrollmentRepository;
    this.lessonScheduleRepository = lessonScheduleRepository;
    this.revisionRepository = revisionRepository;
    this.lessonRepository = lessonRepository;
    this.preferencesRepository = preferencesRepository;
    this.userRepository = userRepository;
    this.idempotencyService = idempotencyService;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional
  public EnrollmentResponse createEnrollment(
      String rawIdempotencyKey, CreateEnrollmentRequest request) {
    UUID userId = currentUser.requireUserId();
    String idempotencyKey = idempotencyService.validateAndNormalizeKey(rawIdempotencyKey);

    // 1. Validate inputs
    if (request.dailyTargetMinutes() < 10 || request.dailyTargetMinutes() > 180) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Mục tiêu phút mỗi ngày phải từ 10 đến 180 phút.");
    }
    if (request.studyDays() == null || request.studyDays().isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Phải chọn ít nhất một ngày học trong tuần.");
    }
    String timeZone = request.timeZone() != null ? request.timeZone().trim() : "";
    try {
      ZoneId.of(timeZone);
    } catch (Exception ex) {
      throw new DomainException(ErrorCode.VALIDATION_FAILED, "Múi giờ IANA không hợp lệ.");
    }

    List<DayOfWeek> normalizedDays = request.studyDays().stream().distinct().sorted().toList();
    String normalizedDaysStr =
        normalizedDays.stream().map(DayOfWeek::name).collect(Collectors.joining(","));

    String requestHash =
        idempotencyService.hashPayload(
            request.curriculumId()
                + "|"
                + request.revisionId()
                + "|"
                + request.startDate()
                + "|"
                + normalizedDaysStr
                + "|"
                + request.dailyTargetMinutes()
                + "|"
                + timeZone);

    // 2. Lock user row for concurrency control within transaction
    userRepository.findByIdForUpdate(userId);

    // 3. Check DB idempotency replay
    Optional<IdempotencyRecord> existing =
        idempotencyService.findExisting(userId, "CREATE_ENROLLMENT", idempotencyKey);
    if (existing.isPresent()) {
      return idempotencyService.replay(existing.get(), requestHash, EnrollmentResponse.class);
    }

    // 4. Check for active/paused enrollment in same curriculum
    boolean hasActiveOrPaused =
        enrollmentRepository.existsByUserIdAndCurriculumIdAndStatusIn(
            userId,
            request.curriculumId(),
            List.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PAUSED));
    if (hasActiveOrPaused) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Người học đã có một lần đăng ký đang học hoặc tạm dừng cho lộ trình này.");
    }

    // 5. Verify revision is PUBLISHED and belongs to curriculum
    CurriculumRevision revision =
        revisionRepository
            .findById(request.revisionId())
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.VALIDATION_FAILED, "Phiên bản lộ trình không tồn tại."));
    if (revision.getStatus() != RevisionStatus.PUBLISHED) {
      throw new DomainException(
          ErrorCode.STATE_CONFLICT,
          "Chỉ phiên bản lộ trình đã xuất bản (PUBLISHED) mới được phép đăng ký.");
    }
    if (!revision.getCurriculum().getId().equals(request.curriculumId())) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED, "Phiên bản không thuộc lộ trình đã chọn.");
    }

    // 6. Query required lessons with stable ordering (phase -> week -> lesson)
    List<Lesson> requiredLessons =
        lessonRepository.findRequiredLessonsByRevisionIdOrdered(revision.getId());

    Instant now = Instant.now(clock);

    // 7. Create Enrollment
    Enrollment enrollment =
        new Enrollment(
            userId,
            request.curriculumId(),
            request.revisionId(),
            request.startDate(),
            timeZone,
            normalizedDays,
            request.dailyTargetMinutes(),
            now);
    enrollment = enrollmentRepository.save(enrollment);

    // 8. Generate initial lesson schedules via pure domain SchedulePlanner
    List<vn.englishlearning.scheduling.domain.SchedulableLesson> schedulableLessons =
        requiredLessons.stream()
            .map(
                l ->
                    new vn.englishlearning.scheduling.domain.SchedulableLesson(
                        l.getId(), l.isRequired()))
            .toList();
    vn.englishlearning.scheduling.domain.SchedulePlan plan =
        vn.englishlearning.scheduling.domain.SchedulePlanner.plan(
            schedulableLessons, request.startDate(), normalizedDays);
    List<LessonSchedule> schedules = new ArrayList<>(plan.plannedLessons().size());
    for (var pl : plan.plannedLessons()) {
      schedules.add(
          new LessonSchedule(
              enrollment.getId(), pl.lessonId(), pl.plannedDate(), pl.plannedDate(), now));
    }
    lessonScheduleRepository.saveAll(schedules);

    // 9. Update user's focusedEnrollmentId in preferences
    LearnerPreferences preferences =
        preferencesRepository
            .findById(userId)
            .orElseGet(
                () ->
                    new LearnerPreferences(
                        userId, request.dailyTargetMinutes(), normalizedDaysStr, timeZone, now));
    preferences.setFocusedEnrollmentId(enrollment.getId(), now);
    preferencesRepository.save(preferences);

    // 10. Assemble response and record idempotency
    EnrollmentResponse response =
        new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getUserId(),
            enrollment.getCurriculumId(),
            enrollment.getRevisionId(),
            enrollment.getStatus(),
            enrollment.getStartDate(),
            enrollment.getTimeZone(),
            enrollment.getStudyDaysList(),
            enrollment.getVersion());

    idempotencyService.record(
        userId, "CREATE_ENROLLMENT", idempotencyKey, requestHash, 201, response);

    return response;
  }
}
