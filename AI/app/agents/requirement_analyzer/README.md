# Requirement Analyzer Agent — Gemini

Project có một agent duy nhất. Nhận TXT/MD, DOCX hoặc PDF có text; tạo source blocks; gọi Gemini structured output; kiểm schema, quote và liên kết; xuất `analysis.json` và báo cáo `report.html` đọc được trong trình duyệt.

## Chạy trên Windows PowerShell (Python 3.11+)

Mở terminal tại thư mục giải nén `requirement-analyzer-gemini`:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
Copy-Item .env.example .env
```

Mở `.env` và điền key của bạn:

```dotenv
GEMINI_API_KEY=your_real_key
GEMINI_MODEL=gemini-flash-latest
```

Key tạo tại https://aistudio.google.com/apikey. Model cấu hình qua `.env` hoặc `--model`; nếu tài khoản không có quyền model này, đổi sang model hỗ trợ structured JSON trong Google AI Studio. Alias latest có thể đổi phiên bản; dùng model ID cố định khi làm đánh giá so sánh.

```powershell
.\.venv\Scripts\python.exe main.py
```

Mở `outputs/latest/report.html`; JSON đầy đủ ở `outputs/latest/analysis.json`. Key chỉ đọc từ môi trường hoặc `.env`, không ghi vào output. Nội dung SRS được gửi đến Gemini khi chạy chế độ thực.

## Dùng SRS của bạn

```powershell
.\.venv\Scripts\python.exe main.py --input "C:\Documents\SRS.docx" --document-id SRS-MYPROJECT --version 1.2 --output outputs/my-srs
```

TXT/MD cần UTF-8. DOCX giữ thứ tự paragraph/table; PDF chỉ nhận text trích xuất, chưa hỗ trợ OCR/hình/sơ đồ. PDF có bố cục bảng phức tạp cần kiểm tra source blocks trong output. Giới hạn 10 MB và 1000000 ký tự toàn tài liệu. Tự chia theo module, 12000 ký tự target mỗi request và context phạm vi tối đa 4000 ký tự. Giữ nguyên bảng UC, không cắt bỏ phần cuối.

## Kiểm thử không cần key

```powershell
.\.venv\Scripts\python.exe main.py --demo
.\.venv\Scripts\python.exe -m pytest tests -q
```

`--demo` chỉ tải fixture sample đi kèm, output luôn ghi `DEMO_FIXTURE`. Đây không phải kết quả gọi Gemini. Không có fallback giả khi API lỗi.

## Linux/macOS

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
cp .env.example .env
# Điền key vào .env
.venv/bin/python main.py
```

## Cấu trúc và nhiệm vụ

- `parser.py`: tạo source blocks, phiên bản, hash.
- `models.py`: schema rule/scenario/issue.
- `prompt.py`: Role, Task, chỉ dẫn UNKNOWN và chống chỉ dẫn nhúng.
- `gemini.py`: google-genai SDK, structured output, timeout và retry tối đa 3 lần cho lỗi tạm thời.
- `validation.py`: kiểm quote, nguồn, ID, rule→scenario, UNKNOWN→issue.
- `report.py`: JSON và HTML có escape chống chèn HTML.

Rule/scenario được AI suy luận; schema/quote validation không chứng minh đúng nghiệp vụ. Kết quả luôn REVIEW_REQUIRED. Không có tool shell, tự sửa SRS hoặc agent khác. Không biến output thành test case chuẩn trước khi review. Lỗi 400: kiểm model/schema/input; 401/403: kiểm key/quyền; 429: quota/rate limit; PDF scan: OCR trước. Không gửi key trong chat.

## Nguồn kỹ thuật

- https://github.com/googleapis/python-genai — SDK và response_schema
- https://ai.google.dev/gemini-api/docs/structured-output — JSON structured output
- https://ai.google.dev/gemini-api/docs/quickstart — khởi tạo Gemini API

## Xác minh

Các bài test local kiểm grounding, liên kết, UNKNOWN, parser DOCX và HTML report. Live Gemini cần key hợp lệ của bạn; package không chứa key và không tuyên bố đã test live.

## V2 chạy SRS truyện tranh đi kèm

```powershell
.\.venv\Scripts\python.exe main.py --input examples/SRS_Website_ban_truyen_tranh_online_UC.docx --document-id SRS-COMIC --version 1.0 --output outputs/comic --resume
```

Gọi Gemini cho từng nhóm module rồi 5 nhóm đối chiếu liên module. Mỗi chunk có checkpoint được validate; resume chỉ dùng lại khi input, model và prompt version khớp. Cross-review chạy lại. Alias latest có thể thay model; dùng model ID cố định cho đánh giá.

Kiểm parser không cần key:

```powershell
.\.venv\Scripts\python.exe main.py --input examples/SRS_Website_ban_truyen_tranh_online_UC.docx --parse-only --output outputs/parsed
```

Xem source_blocks.json và chunk_plan.json. --chunk-chars 16000 tăng budget một chunk; --cross-chars 32000 tăng budget đối chiếu; --skip-cross bỏ đối chiếu và ghi cảnh báo review thủ công. Không cắt bảng nếu vượt budget mà báo lỗi rõ.

ID rule/scenario/issue prefix theo chunk; không tự loại bỏ scenario gần giống. M21 out-of-scope được prompt nhắc rõ; cần reviewer xác nhận tuân thủ ngữ nghĩa. GENERAL chứa mở đầu, NFR và project requirements. Block gửi một lần làm target, có thể lặp làm context. Coverage blocks không đồng nghĩa coverage rules.

analysis.json chỉ ghi khi toàn pipeline hoàn tất. Nếu API lỗi giữa chừng, checkpoint thành công được giữ để resume, không báo toàn bộ thành công. --demo chỉ fixture coupon, không phải Gemini thật.
