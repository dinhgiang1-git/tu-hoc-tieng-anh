# Cách làm việc trong dự án

## Ưu tiên người dùng

- Ưu tiên tốc độ phát triển và luồng dùng được trên web desktop. Mobile hoãn.
- Codex điều phối, nghiệm thu, commit/push từng phần đã đạt; AI công nhân triển khai. Không commit secrets, artifacts phiên đăng nhập hoặc code của task khác chưa nghiệm thu.
- Đọc handoff/task hiện tại và convention BE/FE. Quy trình gọn dưới đây thay các yêu cầu chạy kiểm thử toàn bộ ở mọi lượt trong handoff cũ.

## Triển khai và review gọn

- Hoàn thành một luồng FE–BE–DB dùng được trước; tránh dựng framework/hạ tầng/refactor ngoài yêu cầu.
- Tự quyết định chi tiết triển khai thông thường. Chỉ hỏi khi thiếu quyết định sản phẩm thật sự ảnh hưởng kết quả.
- Một báo cáo bàn giao ngắn: đã làm gì, lệnh/check đã chạy, lỗi còn lại. Không tạo thêm tài liệu/review trung gian nếu không cần.
- Review ưu tiên lỗi chạy thực tế, mất dữ liệu, sai quyền, version/concurrency, sai ngày lịch. Góp ý style/cải tiến chưa ảnh hưởng luồng chính không chặn chuyển bước.
- Codex tự sửa lỗi nhỏ ngay trong lượt nghiệm thu. Chỉ giao lại công nhân khi cần sửa một phần chức năng đáng kể; tối đa hai lượt sửa cho cùng finding, sau đó Codex tự xử lý. Không phát sinh vòng sửa mới vì thay sở thích hoặc mở scope.

## Kiểm thử theo thay đổi

- Mỗi lượt sửa: chạy nhóm test trực tiếp liên quan **một lần**; chỉ chạy lại khi có thay đổi, failure hoặc nghi vấn cụ thể.
- FE có thay đổi code: dùng build (đã gồm TypeScript) làm check chung, không chạy thêm typecheck trùng nếu không có lý do. Lint khi nghiệm thu, không sau mỗi edit.
- UI đơn giản/copy/layout: kiểm tra trực tiếp hoặc test component liên quan; không bắt viết test cho mọi chi tiết CSS/text.
- Business/data change: giữ regression có giá trị cho owner, bảo toàn dữ liệu, version/transaction và tính đúng ngày. Với transaction/SQL/concurrency mới, chạy PostgreSQL integration liên quan; không dùng chỉ Mockito để kết luận.
- Mỗi task: kiểm tra luồng mới và một luồng học cốt lõi khi bị ảnh hưởng. Không mặc định chạy lại toàn bộ Playwright hoặc mọi unit/integration suite.
- Full backend verify + FE suite + E2E rộng tại cuối milestone (ví dụ hết G4), hoặc sớm hơn khi đổi auth/schema/shared infrastructure, có nguy cơ lan rộng hay regression chưa giải thích được.
- Không xóa tests cũ chỉ để giảm thời gian chạy. Không tạo test trùng lặp hoặc chỉ chép lại implementation. Báo chính xác check nào chạy/chưa chạy; không tuyên bố full suite đạt khi chỉ chạy một phần.

## Dùng môi trường hiện có

- E2E được phép dùng trực tiếp app/DB development. Tái dùng FE/BE/DB đang chạy, không dựng lại môi trường cho mỗi lượt.
- Seed/reset đúng IDs hoặc allowlist tài khoản test đã xác định. Không truncate/drop DB, tắt trigger/ràng buộc hoặc sửa dữ liệu học/mật khẩu cá nhân. Giữ JUnit Testcontainers hiện có, không viết lại hạ tầng đã dùng được.
- Giữ nguyên migrations đã áp dụng, secrets, compose dùng chung; chỉ thêm migration khi chức năng cần.
- Không sửa/commit hai file tồn từ trước ngoài task: `docs/DAY2_REVIEW.md`, `backend/src/main/resources/META-INF/spring/`.
