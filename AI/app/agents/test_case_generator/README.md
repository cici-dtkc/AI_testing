# Test Case Generator Agent

Sinh test case từ `analysis.json` (rules + scenarios do Requirement Analyzer tạo), có
**engine chọn kỹ thuật** chạy trước LLM. Tri thức kiểm thử lấy từ hai tài liệu:

- `5.Kiemthuhopden.pdf` — hộp đen: phân vùng tương đương (EP), phân tích giá trị biên
  (BVA), bảng quyết định, chuyển đổi trạng thái, use case.
- `6-Kiemthuhoptrang.pdf` — hộp trắng: đường cơ sở McCabe, bao phủ câu lệnh/nhánh/điều
  kiện, kiểm thử dòng dữ liệu (def-use).

## Luồng chạy

```
analysis.json ──► (1) Technique Planner ──► technique_plan ──► (2) Gemini ──► test_cases
                                                                    │
                                                                    ▼
                                                              (3) Validation
```

1. **Technique Planner** (`generator/techniques.py`) — quyết định kỹ thuật chính
   (`PRIMARY`) + tối đa 2 kỹ thuật bổ trợ (`SUPPORTING`) cho từng module, kèm nghĩa vụ
   bao phủ và số case ước tính. Chạy offline, không phụ thuộc LLM.
2. **Gemini** (`generator/gemini.py`) — gọi **mỗi module/hàm một request** để model
   không bị ngợp input lớn và phải phủ hết scenario của module; module lớn còn được
   **bù thiếu** thêm một request nhỏ cho các scenario bị bỏ sót; sau đó gộp kết quả,
   chuẩn hoá cột Luồng, loại ID truy vết không có trong analysis và đánh số lại ID.
   Có nghỉ giữa các request (`--delay`, mặc định 2s) và retry/backoff khi gặp 429/5xx.
3. **Validation** (`generator/validation.py`) — kiểm tra truy vết, đúng kế hoạch kỹ thuật,
   single-fault assumption, trùng coverage item, độ phủ scenario/rule.

## Khi nào áp dụng kỹ thuật nào

Thang quyết định trong `select_techniques()` (dừng ở nhánh đầu tiên khớp):

| # | Điều kiện nhận diện | Kỹ thuật chính | Căn cứ |
|---|---|---|---|
| R1 | Toàn bộ rule là `NON_FUNCTIONAL` (thời gian phản hồi, mã hoá, tải...) | `NON_FUNCTIONAL_METRIC` | Ngưỡng đo được, không có logic nghiệp vụ |
| R2 | Có scenario `AUTHORIZATION` hoặc tín hiệu phân quyền ở ≥50% rule | `DECISION_TABLE` (ma trận vai trò × hành động) | Bảng quyết định |
| R3 | Có scenario `STATE` hoặc ≥3 tín hiệu vòng đời ở ≥50% rule | `STATE_TRANSITION` | Chuyển đổi trạng thái |
| R4 | Rule có ≥2 điều kiện, ≥2 kết quả khác nhau, là `BUSINESS_RULE` | `DECISION_TABLE` | Bảng quyết định |
| R5 | Có ràng buộc số/độ dài có biên (min/max) | `BOUNDARY_VALUE_ANALYSIS` | Phân tích giá trị biên |
| R6 | ≥2 tín hiệu luồng (đăng nhập, đặt hàng, thanh toán...) và ≥2 scenario | `USE_CASE` | Kiểm thử use case |
| R7 | Không có tín hiệu đặc thù | `EQUIVALENCE_PARTITIONING` | Phân vùng tương đương |

Quy tắc phụ:

- **Tín hiệu phải là đặc trưng module**: một vài câu nhắc "trạng thái" trong module 16
  rule không đủ để chọn State Transition (`_dominant()` yêu cầu ≥50% số rule).
- **EP luôn là lưới an toàn**: khi kỹ thuật chính không phải EP, EP được thêm làm bổ trợ
  để không bỏ sót lớp hợp lệ/không hợp lệ.
- **NFR được ưu tiên trong danh sách bổ trợ**: rule phi chức năng cần case đo ngưỡng
  riêng nên được chèn trước USE_CASE, tránh bị cắt khi chỉ giữ 3 kỹ thuật bổ trợ.
- **BVA bổ trợ dùng mức nhẹ** (`TWO_VALUE`/`THREE_VALUE`), tránh bùng nổ `5^n`.
- Biến thể EP: `WEAK_NORMAL` hoặc `WEAK_ROBUST` (khi có scenario NEGATIVE / tín hiệu
  kiểm tra dữ liệu). `STRONG_NORMAL` và `STRONG_ROBUST` không được chọn tự động vì số
  case tăng theo tích Descartes; dùng khi có công cụ sinh tổ hợp.
- Biến thể BVA theo tài liệu: `TWO_VALUE` = 4n+1, `THREE_VALUE` = 6n+1,
  `WORST_CASE` = 5^n, `ROBUST_WORST_CASE` = 7^n. `n` là số biến có ràng buộc biên
  (một rule như `1 ≤ a, b, c ≤ 200` được tính là 3 biến). Worst-case chỉ dùng khi có
  từ 2 biến trong cùng rule trở lên; nếu `5^n`/`7^n > 60`, engine tự hạ xuống
  `THREE_VALUE` và ghi cảnh báo.

## Kiểm thử hộp trắng (tuỳ chọn)

Truyền `--code <file|thư mục>` để phân tích mã nguồn Python bằng `ast`:

- `V(G) = số điểm quyết định + 1` (công thức McCabe trong tài liệu).
- Hạng mục bao phủ mức 1–4 (câu lệnh, nhánh, điều kiện, kết hợp) và số cặp def-use.
- Số test case tối thiểu = `V(G)` cho Basis Path.
- Mã nguồn mỗi hàm (giới hạn 12.000 ký tự) được đưa vào prompt để LLM viết case hộp trắng.

## Cách dùng

```bash
# Chỉ xem engine chọn kỹ thuật gì (không gọi Gemini, không cần API key)
python main.py --plan-only

# Sinh test case (cần GEMINI_API_KEY trong .env)
python main.py

# Kèm phân tích hộp trắng từ mã nguồn
python main.py --code ../risk_prioritization/core

# Chỉ định input/output khác
python main.py --input outputs/comic/analysis.json --output outputs/comic/test_cases

# Tăng khoảng nghỉ giữa các request nếu bị 429 (rate limit theo phút)
python main.py --delay 5

# Xoá cache từng module và sinh lại từ đầu
python main.py --fresh
```

Mỗi module sinh xong được lưu vào `<output>/.module_cache/` (kèm fingerprint của
analysis + plan). Nếu hết quota giữa chừng, chương trình dừng êm, vẫn xuất các case đã
có và báo cảnh báo; chạy lại sẽ dùng cache và tiếp tục từ module còn thiếu, không gọi
lại API cho phần đã xong. Đổi model không làm mất cache.

### Sự cố thường gặp

- `Gemini 429 ... limit: 0 ...`: model đang đặt trong `.env` **không có quota Free
  Tier**, retry bao nhiêu cũng không được. Đổi `GEMINI_MODEL` sang
  `gemini-flash-latest` / `gemini-flash-lite-latest` hoặc chạy `--model
  gemini-flash-latest`.
- `Gemini 429 ... quota/rate limit`: hết quota tạm thời — chờ khoảng 1 phút, tăng
  `--delay`, hoặc đổi model.
- Model cũ như `gemini-2.5-flash` có thể trả 404 với tài khoản mới; dùng các model
  `*-latest` ở trên.

Kết quả trong thư mục output:

- `test_cases.json` — test suite + `technique_plan` + `validation_warnings`
- `test_cases_report.html` — bảng test case theo mẫu bài tập 8: `Mã TC · Luồng ·
  Dữ liệu đầu vào · Các bước · Kết quả mong đợi · Kịch bản` (kèm Kỹ thuật, Coverage,
  Ưu tiên). Tên module nằm ngay trong mã test case (`TimKiem-TC-01`), không lặp cột
  Module. Các bước viết 2–5 bước, mỗi bước ≤ 10 từ; kết quả mong đợi 1 câu ≤ 25 từ.
  Cuối báo cáo có 2 bảng kế hoạch (hộp đen theo module, hộp trắng theo hàm).
- `technique_plan.json` — chỉ sinh ở chế độ `--plan-only`

## Quy ước cột Luồng

Mọi test case đều có Luồng, theo đúng mẫu bài 8:

| Giá trị | Ý nghĩa |
|---|---|
| `Chính` | Luồng chính — hành vi đúng/happy path |
| `E1`, `E2`… | Luồng ngoại lệ; trong một module các case cùng nhóm lỗi dùng cùng mã, các nhóm đánh số tăng dần |
| `Ẩn` | Luồng bổ sung ngoài sơ đồ (case phi chức năng, case hộp trắng, chống trùng lặp/click đúp…) |

Ví dụ nhóm lỗi ở bài 8: SĐT 9 số / 11 số / chứa chữ đều là `E1`, mã giảm giá sai
hoặc hết hạn là `E2`, số dư thẻ là `E3`, tồn kho là `E4`, click đúp nút đặt hàng là `Ẩn`.

## Quy ước coverage item

`Vn` lớp hợp lệ · `Xn` lớp không hợp lệ · `Bn` biên hợp lệ · `Dn` biên không hợp lệ ·
`Rn` rule bảng quyết định · `Tn` chuyển đổi trạng thái · `Fn` luồng use case ·
`Pn` đường cơ sở · `Sn` câu lệnh · `DFn` cặp def-use.

Mỗi test case chỉ được chứa **tối đa một** tag không hợp lệ (`X`/`D`) để tránh fault
masking; mỗi tag chỉ dùng ở một test case trong cùng module.

## Test

```bash
python -m pytest tests -q
```
