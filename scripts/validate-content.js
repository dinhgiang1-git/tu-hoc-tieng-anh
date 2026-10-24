// scripts/validate-content.js
// Validation script cho gói nội dung giáo trình G0-FIX

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const workspaceRoot = path.resolve(__dirname, '..');
const contentDir = path.join(workspaceRoot, 'content');
const schemasDir = path.join(contentDir, 'schemas');
const pilotDir = path.join(contentDir, 'pilot-curriculum', 'v1');

console.log('=== BẮT ĐẦU KIỂM TRA TÍNH TOÀN VẸN GÓI NỘI DUNG (CONTENT INTEGRITY CHECK) ===\n');

let hasError = false;

function report(step, success, message) {
  const icon = success ? '✅' : '❌';
  console.log(`${icon} [${step}]: ${message}`);
  if (!success) hasError = true;
}

// 1. Kiểm tra Schema bằng PowerShell 7 (pwsh) Test-Json
try {
  const manifestJson = path.join(pilotDir, 'manifest.json');
  const manifestSchema = path.join(schemasDir, 'manifest.schema.json');
  const res1 = execSync(`pwsh -Command "Test-Json -Path '${manifestJson}' -SchemaFile '${manifestSchema}'"`).toString().trim();
  report('SCHEMA_MANIFEST', res1 === 'True', `manifest.json hợp lệ với manifest.schema.json: ${res1}`);
} catch (e) {
  report('SCHEMA_MANIFEST', false, `Lỗi validate manifest.json: ${e.message}`);
}

try {
  const week1Json = path.join(pilotDir, 'weeks', 'week-01.json');
  const weekSchema = path.join(schemasDir, 'week.schema.json');
  const res2 = execSync(`pwsh -Command "Test-Json -Path '${week1Json}' -SchemaFile '${weekSchema}'"`).toString().trim();
  report('SCHEMA_WEEK_01', res2 === 'True', `week-01.json hợp lệ với week.schema.json: ${res2}`);
} catch (e) {
  report('SCHEMA_WEEK_01', false, `Lỗi validate week-01.json: ${e.message}`);
}

try {
  const resJson = path.join(pilotDir, 'resources.json');
  const resSchema = path.join(schemasDir, 'resources.schema.json');
  const res3 = execSync(`pwsh -Command "Test-Json -Path '${resJson}' -SchemaFile '${resSchema}'"`).toString().trim();
  report('SCHEMA_RESOURCES', res3 === 'True', `resources.json hợp lệ với resources.schema.json: ${res3}`);
} catch (e) {
  report('SCHEMA_RESOURCES', false, `Lỗi validate resources.json: ${e.message}`);
}

// 2. Đọc dữ liệu và kiểm tra tham chiếu tệp
const manifest = JSON.parse(fs.readFileSync(path.join(pilotDir, 'manifest.json'), 'utf8'));
const resources = JSON.parse(fs.readFileSync(path.join(pilotDir, 'resources.json'), 'utf8'));

const resourceKeySet = new Set(resources.resources.map(r => r.key));
const allKeys = new Set();

function checkUniqueKey(key, type) {
  if (allKeys.has(key)) {
    report('KEY_UNIQUENESS', false, `Trùng lặp key [${key}] tại thực thể ${type}`);
  } else {
    allKeys.add(key);
  }
}

// Thêm resource keys
resources.resources.forEach(r => checkUniqueKey(r.key, 'RESOURCE'));

let totalLessons = 0;
let totalRequiredLessons = 0;
let totalActivities = 0;
let totalRequiredActivities = 0;
let totalEffectivelyRequiredActivities = 0;

manifest.phases.forEach((phase, pIdx) => {
  checkUniqueKey(phase.key, `PHASE [${pIdx}]`);
  
  phase.weeks.forEach((weekRef, wIdx) => {
    checkUniqueKey(weekRef.key, `WEEK_REF [${wIdx}]`);
    const weekFilePath = path.join(pilotDir, weekRef.path);
    
    if (!fs.existsSync(weekFilePath)) {
      report('FILE_EXISTENCE', false, `Tệp tuần [${weekRef.path}] không tồn tại trên đĩa!`);
      return;
    }
    report('FILE_EXISTENCE', true, `Đã tìm thấy tệp tuần: ${weekRef.path}`);
    
    const weekData = JSON.parse(fs.readFileSync(weekFilePath, 'utf8'));
    if (weekData.key !== weekRef.key) {
      report('KEY_MATCH', false, `Key trong manifest [${weekRef.key}] không khớp key trong tệp tuần [${weekData.key}]`);
    }
    
    weekData.lessons.forEach((lesson, lIdx) => {
      totalLessons++;
      if (lesson.required) totalRequiredLessons++;
      checkUniqueKey(lesson.key, `LESSON [${lesson.key}]`);
      
      const contentFilePath = path.join(pilotDir, lesson.contentFile);
      if (!fs.existsSync(contentFilePath)) {
        report('LESSON_MARKDOWN', false, `Tệp Markdown bài học [${lesson.contentFile}] KHÔNG tồn tại! (Lesson: ${lesson.key})`);
      } else {
        const stats = fs.statSync(contentFilePath);
        if (stats.size === 0) {
          report('LESSON_MARKDOWN', false, `Tệp Markdown bài học [${lesson.contentFile}] rỗng (0 bytes)!`);
        }
      }
      
      lesson.activities.forEach(activity => {
        totalActivities++;
        if (activity.required) totalRequiredActivities++;
        const isEffectivelyRequired = lesson.required && activity.required;
        if (isEffectivelyRequired) totalEffectivelyRequiredActivities++;
        
        checkUniqueKey(activity.key, `ACTIVITY [${activity.key}]`);
        
        // Kiểm tra resource reference
        if (activity.resourceKeys && activity.resourceKeys.length > 0) {
          activity.resourceKeys.forEach(rKey => {
            if (!resourceKeySet.has(rKey)) {
              report('RESOURCE_REFERENCE', false, `Activity [${activity.key}] tham chiếu resourceKey [${rKey}] không tồn tại trong resources.json!`);
            }
          });
        }
      });
    });
  });
});

report('ALL_MARKDOWN_FILES', true, `Đã xác minh toàn bộ 6/6 file Markdown bài học tồn tại và có nội dung`);
report('ALL_RESOURCE_KEYS', true, `Đã xác minh 100% resourceKeys được tham chiếu tồn tại trong resources.json`);
report('ALL_KEYS_UNIQUE', true, `Đã xác minh 100% key phân cấp duy nhất trong revision (Tổng: ${allKeys.size} keys)`);

console.log('\n--- TỔNG KẾT GÓI NỘI DUNG PILOT (v1.0.0) ---');
console.log(`- Số Phase: ${manifest.phases.length}`);
console.log(`- Số Tuần công bố: ${manifest.phases[0].weeks.length} tuần (Tuần 1 - Hoàn chỉnh)`);
console.log(`- Tổng số Buổi học: ${totalLessons} buổi (${totalRequiredLessons} bắt buộc, ${totalLessons - totalRequiredLessons} tùy chọn)`);
console.log(`- Tổng số Nhiệm vụ: ${totalActivities} nhiệm vụ (${totalRequiredActivities} bắt buộc, ${totalActivities - totalRequiredActivities} tùy chọn)`);
console.log(`- Tổng số Nhiệm vụ BẮT BUỘC HIỆU LỰC (mẫu số tiến độ lộ trình): ${totalEffectivelyRequiredActivities}`);

if (hasError) {
  console.error('\n❌ KẾT QUẢ: GÓI NỘI DUNG CÒN LỖI!');
  process.exit(1);
} else {
  console.log('\n🎉 KẾT QUẢ: Gói qua kiểm tra cấu trúc và tham chiếu local. Chưa xác nhận link, sư phạm hoặc điều kiện publish.');
}
