# Bug Report Agent (Comic Store QA)

## 1. Giới thiệu
**Bug Report Agent** là Agent trong chuỗi Multi-Agent Testing của dự án Comic Store, chịu trách nhiệm tổng hợp kết quả kiểm thử từ Test Runner và các phân tích nguyên nhân từ Failure Analysis Agent để lập báo cáo lỗi (Bug Report) hoàn chỉnh, chuẩn hóa và có khả năng truy vết cao.

---

## 2. Hợp đồng dữ liệu (Data Contract)

### 2.1. Đầu vào (Inputs mong đợi từ Failure Analysis Agent & Test Runner):
- `failureId`: Mã phân tích lỗi (ví dụ: `FA-001`).
- `testId`: Mã ca kiểm thử gặp lỗi (ví dụ: `TC-02`).
- `classification`: Phân loại lỗi (`PRODUCT_BUG` là điều kiện cần để tạo Bug Report).
- `confidence`: Độ tin cậy phân loại (ngưỡng $\ge 0.6$).
- `suspectedComponent`: Thành phần backend/frontend bị nghi ngờ.
- `observedFacts`: Các sự thật ghi nhận khách quan kèm mã tham chiếu bằng chứng.
- `hypotheses`: Các giả thuyết nguyên nhân kỹ thuật.
- `evidence`: Danh sách bằng chứng (Screenshot, API Response, Log, SHA-256).

### 2.2. Đầu ra (Output JSON chuẩn của Bug Report):
Tệp mẫu lưu tại: `outputs/sample_bug_report.json`.
Bao gồm các trường cốt lõi:
- `bugId`, `title`, `module`, `feature`, `environment`
- `preconditions`, `steps` (kèm Trace Playwright), `expected`, `actual`
- `severity` & `factors` ($I, M, W, S$)
- `observedFacts` vs `suspectedCauses` (tách bạch rõ ràng)
- `traceability` (nối ngược `testId`, `requirementScenarioId`, `businessRuleId`, `failureId`)

---

## 3. Công thức tính mức độ nghiêm trọng (Severity Formula)

Bug Report Agent tự tính điểm Severity bằng công thức tường minh:
$$\text{Score} = I + M + W + S \quad (\text{Thang điểm từ } 0 \text{ đến } 10)$$

Trong đó:
- **$I$ (Impact)**: Mức độ ảnh hưởng chức năng ($0 - 4$)
- **$M$ (Money / Cart Data)**: Rủi ro tài chính, giỏ hàng, tồn kho ($0 - 3$)
- **$W$ (Workaround)**: Khả năng né tránh lỗi ($0 - 2$)
- **$S$ (Scope)**: Phạm vi người dùng / sản phẩm bị ảnh hưởng ($0 - 1$)

### Bảng ánh xạ điểm số:
- **CRITICAL**: $7 - 10$ điểm
- **HIGH**: $5 - 6$ điểm
- **MEDIUM**: $3 - 4$ điểm
- **LOW**: $0 - 2$ điểm

---

## 4. Cơ chế Guardrails & Human-in-the-loop
1. **Lỗi không phải Product Bug:** Nếu `classification != "PRODUCT_BUG"` hoặc `confidence < 0.6`, Agent không tạo bug report mà ghi nhận vào log cảnh báo.
2. **Tách biệt Sự thật & Giả thuyết:** Không được trộn lẫn giữa `observedFacts` (đã chứng minh bằng log/ảnh) và `suspectedCauses` (dự đoán nguyên nhân).
3. **Human Override:** Cho phép QA / PO can thiệp điều chỉnh mức Severity qua giao diện, mọi thay đổi được lưu dấu `[Human Override]` vào Audit Log.

---

## 5. Hướng dẫn chạy thử nghiệm độc lập (Standalone)
Từ thư mục gốc dự án:
```bash
python -m app.agents.bug_report.main
```

---

## 6. Hợp đồng bàn giao cho Regression Selection Agent (Agent tiếp theo)

Sau khi Bug Report Agent hoàn thành việc đóng gói lỗi, **Regression Selection Agent** sẽ sử dụng đầu ra này để chọn lọc các kịch bản kiểm thử hồi quy cần chạy lại sau khi lập trình viên sửa code.

### 6.1. Tệp bàn giao chính thức:
* **Tệp tất cả báo cáo lỗi:** `outputs/generated_bugs/all_bug_reports_summary.json` (30 lỗi).
* **Tệp lỗi sản phẩm chính thức (Đã qua Gate Check):** `outputs/generated_bugs/official_product_bugs.json` (2 ca `PRODUCT_BUG` cần Dev sửa).

### 6.2. Các trường dữ liệu quan trọng dành cho Regression Agent:
| Trường | Kiểu dữ liệu | Ý nghĩa đối với Regression Selection Agent |
| :--- | :--- | :--- |
| `testId` | `string` | Mã test case bị gãy trực tiếp, bắt buộc phải chọn để chạy lại (ví dụ: `TimKiem-TC-04`). |
| `module` | `string` | Phân hệ bị ảnh hưởng (ví dụ: `TimKiem`), dùng để tìm kiếm các test case lân cận cùng module. |
| `ruleId` | `string` | Quy tắc nghiệp vụ bị vi phạm (ví dụ: `C003-RULE-M02-001`), dùng để chạy regression các case chung rule. |
| `severity` | `string` | Mức độ nghiêm trọng (`CRITICAL`, `HIGH`...) để xếp thứ tự ưu tiên test case nào chạy trước. |
| `defectType` | `string` | `PRODUCT_BUG` (Cần Dev sửa code ứng dụng) vs `TEST_MAINTENANCE` (Cần sửa kịch bản test). |

### 6.3. Đoạn mã Python mẫu để Regression Agent nạp dữ liệu:
```python
import json
from pathlib import Path

# Đọc danh sách bug cần sửa từ Bug Report Agent
bugs_path = Path("app/agents/bug_report/outputs/generated_bugs/official_product_bugs.json")
with open(bugs_path, "r", encoding="utf-8") as f:
    product_bugs = json.load(f)

# Lấy danh sách test case ID và module để chọn hồi quy
regression_target_tests = [b["testId"] for b in product_bugs]
impacted_modules = list(set([b["module"] for b in product_bugs]))

print(f"Test cases cần chạy hồi quy: {regression_target_tests}")
print(f"Phân hệ bị ảnh hưởng cần quét thêm test: {impacted_modules}")
```
