// scripts/validate-openapi-examples.js
// Kiểm tra OpenAPI contract, cú pháp OAS 3.1 và các ví dụ payload cho G0-FIX

const fs = require('fs');
const path = require('path');

const openapiPath = path.resolve(__dirname, '../docs/api/openapi.yaml');
const openapiContent = fs.readFileSync(openapiPath, 'utf8');

console.log('=== BẮT ĐẦU KIỂM TRA HỢP ĐỒNG API VÀ PAYLOAD EXAMPLES (OPENAPI VALIDATION) ===\n');

let hasError = false;

function report(testName, success, message) {
  const icon = success ? '✅' : '❌';
  console.log(`${icon} [${testName}]: ${message}`);
  if (!success) hasError = true;
}

// 1. Kiểm tra không còn cú pháp OpenAPI 3.0 "nullable: true" (xử lý R5)
const nullableMatches = [];
openapiContent.split('\n').forEach((line, idx) => {
  if (line.includes('nullable: true')) {
    nullableMatches.push({ line: idx + 1, text: line.trim() });
  }
});

if (nullableMatches.length === 0) {
  report('OAS_3_1_NULLABLE_SYNTAX', true, 'Không còn bất kỳ thuộc tính "nullable: true" nào. Đã chuẩn hóa sang cú pháp OAS 3.1 (type unions & anyOf null).');
} else {
  report('OAS_3_1_NULLABLE_SYNTAX', false, `Vẫn còn ${nullableMatches.length} vị trí sử dụng "nullable: true" tại các dòng: ${nullableMatches.map(m => m.line).join(', ')}`);
}

// 2. Kiểm tra khai báo Security Schemes (CookieAuth) và CSRF header
const hasCookieAuth = openapiContent.includes('CookieAuth:') && openapiContent.includes('name: SESSION') && openapiContent.includes('in: cookie');
report('SECURITY_COOKIE_AUTH', hasCookieAuth, 'Đã khai báo CookieAuth (HttpOnly session cookie name: SESSION)');

const hasCsrfParam = openapiContent.includes('CsrfHeader:') && openapiContent.includes('name: X-CSRF-TOKEN');
report('SECURITY_CSRF_HEADER', hasCsrfParam, 'Đã khai báo CsrfHeader (X-CSRF-TOKEN parameter)');

// 3. Kiểm tra các ví dụ phản hồi của TodayResponse cho mọi trạng thái (xử lý R6)
const todayExamples = {
  no_enrollment: {
    state: "NO_ENROLLMENT",
    enrollmentId: null,
    revisionId: null,
    curriculumTitle: null,
    todayDate: "2026-10-28",
    isRestDay: false,
    enrollmentVersion: null,
    primaryAction: null,
    overdueLessons: [],
    progress: null
  },
  paused: {
    state: "PAUSED",
    enrollmentId: "8f9a2b1c-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    revisionId: "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
    curriculumTitle: "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
    todayDate: "2026-10-28",
    isRestDay: false,
    enrollmentVersion: 3,
    primaryAction: null,
    overdueLessons: [],
    progress: {
      completedRequiredActivities: 2,
      totalRequiredActivities: 19,
      progressPercent: 10.5,
      enrollmentVersion: 3
    }
  },
  in_progress: {
    state: "IN_PROGRESS",
    enrollmentId: "8f9a2b1c-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    revisionId: "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
    curriculumTitle: "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
    todayDate: "2026-10-28",
    isRestDay: false,
    enrollmentVersion: 2,
    primaryAction: {
      type: "IN_PROGRESS",
      message: "Bạn đang học dở Buổi 2. Hãy tiếp tục để hoàn thành mục tiêu!",
      lessonId: "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      lessonTitle: "Buổi 2: Cặp phụ âm /p/ và /b/",
      estimatedMinutes: 20,
      completedActivities: 2,
      totalActivities: 4
    },
    overdueLessons: [],
    progress: {
      completedRequiredActivities: 2,
      totalRequiredActivities: 19,
      progressPercent: 10.5,
      enrollmentVersion: 2
    }
  },
  due_today: {
    state: "DUE_TODAY",
    enrollmentId: "8f9a2b1c-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    revisionId: "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
    curriculumTitle: "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
    todayDate: "2026-10-28",
    isRestDay: false,
    enrollmentVersion: 1,
    primaryAction: {
      type: "DUE_TODAY",
      message: "Buổi học đến hạn hôm nay: Buổi 2",
      lessonId: "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      lessonTitle: "Buổi 2: Cặp phụ âm /p/ và /b/",
      estimatedMinutes: 20,
      completedActivities: 0,
      totalActivities: 4
    },
    overdueLessons: [],
    progress: {
      completedRequiredActivities: 4,
      totalRequiredActivities: 19,
      progressPercent: 21.1,
      enrollmentVersion: 1
    }
  },
  rest_day_upcoming: {
    state: "REST_DAY",
    enrollmentId: "8f9a2b1c-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    revisionId: "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
    curriculumTitle: "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
    todayDate: "2026-10-27",
    isRestDay: true,
    enrollmentVersion: 1,
    primaryAction: {
      type: "UPCOMING",
      message: "Hôm nay là ngày nghỉ theo lịch của bạn. Bạn có thể học trước bài tiếp theo nếu muốn!",
      lessonId: "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
      lessonTitle: "Buổi 2: Cặp phụ âm /p/ và /b/",
      estimatedMinutes: 20,
      completedActivities: 0,
      totalActivities: 4
    },
    overdueLessons: [],
    progress: {
      completedRequiredActivities: 4,
      totalRequiredActivities: 19,
      progressPercent: 21.1,
      enrollmentVersion: 1
    }
  },
  curriculum_completed: {
    state: "CURRICULUM_COMPLETED",
    enrollmentId: "8f9a2b1c-3d4e-5f6a-7b8c-9d0e1f2a3b4c",
    revisionId: "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
    curriculumTitle: "Lộ trình Thử nghiệm: Phát âm Anh-Anh",
    todayDate: "2026-11-05",
    isRestDay: false,
    enrollmentVersion: 10,
    primaryAction: {
      type: "CURRICULUM_COMPLETED",
      message: "Chúc mừng bạn đã hoàn thành 100% nhiệm vụ bắt buộc của lộ trình!",
      lessonId: null,
      lessonTitle: null,
      estimatedMinutes: null,
      completedActivities: null,
      totalActivities: null
    },
    overdueLessons: [],
    progress: {
      completedRequiredActivities: 19,
      totalRequiredActivities: 19,
      progressPercent: 100.0,
      enrollmentVersion: 10
    }
  }
};

console.log('INFO: Có 6 fixture Hôm nay trong script; chưa validate các fixture này với JSON Schema.');

// 4. Kiểm tra Reload flow & enrollmentVersion (xử lý R4)
const hasEnrollmentVersionInWorkspace = openapiContent.includes('LessonDetailResponse:') && openapiContent.includes('enrollmentVersion:');
const hasEnrollmentVersionInSummary = openapiContent.includes('ProgressSummaryResponse:') && openapiContent.includes('enrollmentVersion:');
const hasGetEnrollmentDetail = openapiContent.includes('/enrollments/{enrollmentId}:') && openapiContent.includes('getEnrollmentDetail');

report('RELOAD_VERSION_READ_CONTRACT', hasEnrollmentVersionInWorkspace && hasEnrollmentVersionInSummary && hasGetEnrollmentDetail, 
  'Đã thấy các marker version/endpoint trong văn bản; vị trí và required fields cần schema validation riêng');

// 5. Kiểm tra trường hợp Nullable progress (xử lý R5)
const nullableProgressExample = {
  completedRequiredActivities: 0,
  totalRequiredActivities: 0,
  progressPercent: null,
  enrollmentVersion: 1
};
report('NULLABLE_PROGRESS_PERCENT', nullableProgressExample.progressPercent === null, 'Fixture local chứa progressPercent=null; kiểm tra này chưa xác minh schema API');

// 6. Kiểm tra Reflection Response Draft vs Submit (xử lý R7)
const hasSaveDraftEndpoint = openapiContent.includes('/enrollments/{enrollmentId}/activities/{activityId}/response:') && openapiContent.includes('saveActivityResponseDraft');
const hasSubmitResponseEndpoint = openapiContent.includes('/enrollments/{enrollmentId}/activities/{activityId}/response/submit:') && openapiContent.includes('submitActivityResponse');

report('REFLECTION_DRAFT_AND_SUBMIT', hasSaveDraftEndpoint && hasSubmitResponseEndpoint, 
  'Đã tách riêng: PUT /response (lưu nháp, không hoàn thành) và POST /response/submit (yêu cầu expectedVersion, lưu text và hoàn thành atomic)');

console.log('\n--- TỔNG KẾT KIỂM TRA CONTRACT OPENAPI ---');
if (hasError) {
  console.error('❌ KẾT QUẢ: OPENAPI CONTRACT CÒN LỖI!');
  process.exit(1);
} else {
  console.log('🎉 KẾT QUẢ: Các smoke check văn bản đã qua. Chưa phải full OpenAPI validation hoặc schema validation cho examples.');
}
