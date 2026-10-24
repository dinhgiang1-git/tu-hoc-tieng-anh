# Giao AI triển khai G2-01 — Gói nội dung và importer

## Điểm bắt đầu

Phần FE G1-03 đã được chốt: 24/24 test chính, 3/3 test tái hiện, typecheck/lint/build đạt. Cảnh báo bundle 528.89 kB không chặn chuyển task. Backend integration cần Docker đang chạy để xác minh lại; không dùng DB dev làm phương án thay thế. Không mở lại auth, Agent Desk hoặc thiết kế giao diện trong task này.

## Prompt có thể gửi nguyên văn

```text
Triển khai Task G2-01 trong D:\WorkSpace\tu-hoc-tieng-anh.

Đọc BE_CODE_CONVENTION.md, docs/tasks/backlog.md (G2-01),
docs/product-rules.md, docs/adr/ADR-005-content-package-versioning-and-verification.md,
content/schemas và gói content/pilot-curriculum/v1.

Chỉ làm backend nội dung, validator, dry-run, importer và publish:

1. Migration số kế tiếp hiện là V003; kiểm tra lại thư mục migration trước
khi tạo. Không sửa V001/V002, không nhét nội dung bài học vào migration.
Tạo schema quan hệ curriculum/revision/phase/week/lesson/activity/resource
và liên kết activity-resource, có constraints/key/index phù hợp.

2. Validator phải dùng JSON Schema thực sự cho manifest, week và resources;
kiểm tra key trùng, tham chiếu tuần/resource, file Markdown tồn tại và đường
dẫn nằm trong package. Chặn absolute path, traversal và symlink trỏ ra ngoài.
Không tự động tải tài nguyên ngoài hoặc đổi trạng thái UNVERIFIED_DRAFT.
Chỉ import các tuần được manifest tham chiếu; không gom drafts/.

3. Có CLI validate, dry-run, import, publish. Validate/dry-run không ghi DB.
Luồng CLI không khởi động web server hoặc bootstrap tài khoản không liên quan.
Thiết kế cấu hình DB tường minh; không tự chạy import/publish vào DB dev.

4. Import toàn bộ package trong một transaction, trạng thái DRAFT. Checksum
phải bao gồm cả nội dung Markdown và resources được nhập, không chỉ manifest.
Import lại cùng package không tạo bản ghi trùng; cùng key/version nhưng nội
dung khác phải báo conflict rõ, không âm thầm sửa revision đã publish.

5. Publish chỉ từ DRAFT hợp lệ; revision PUBLISHED và nội dung con bất biến.
Giữ provenance/verificationStatus. Báo rõ cảnh báo tài nguyên chưa xác minh;
không mô tả pilot là bản sao đã xác thực đầy đủ của Google Sheet nguồn.

Nghiệm thu theo dữ liệu thực tế hiện có:
- 1 phase, 1 tuần, 6 lessons: 5 bắt buộc và 1 tùy chọn.
- 21 activities, 19 activities bắt buộc hiệu lực theo lesson.required
  kết hợp activity.required. Không tính buổi tùy chọn thành bắt buộc.
- Validate/dry-run báo đúng số lượng và không ghi DB.
- Import lặp không nhân đôi; lỗi giữa chừng rollback toàn bộ.
- File/tham chiếu/key lỗi bị chặn; file ngoài package không được đọc.
- Published revision không sửa được; thay đổi Markdown làm checksum thay đổi.

Kiểm thử bằng PostgreSQL Testcontainers biệt lập. Docker chưa chạy thì ghi
rõ phần nào chưa xác minh, không fallback DB dev, không chỉnh Docker/WSL.
Chạy Spotless và Maven verify khi runtime sẵn sàng; không ghi pass cho test
chưa chạy. Cập nhật backlog và tài liệu CLI bằng lệnh thực tế đã kiểm tra.

Không làm G2-02/catalog UI, admin, enrollment, tiến độ hay phát âm ở task này.
Không sửa lại F1/F2/R1/R2 đã chốt nếu không có regression mới chứng minh lỗi.
Không commit/push. Dừng khi G2-01 có code, tests và báo cáo để review;
không tự mở rộng task tiếp theo.
```
