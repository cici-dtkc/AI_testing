# Failure Analyzer Agent — đặc tả đầu vào và đầu ra

## 1. Mục đích và phạm vi

Failure Analyzer phân tích một lần chạy kiểm thử thất bại để:

- xác định test nào thất bại, bị chặn, lỗi môi trường hoặc có dấu hiệu flaky;
- đối chiếu kết quả thực tế với test case, yêu cầu và mức ưu tiên nếu có;
- đưa ra nguyên nhân khả dĩ, bằng chứng, độ tin cậy và bước xử lý tiếp theo;
- cung cấp dữ liệu có cấu trúc cho supervisor quyết định retry hay kết thúc.

Agent **không** tự sửa mã, tự chạy lại test, tự thay đổi test case/yêu cầu, hoặc khẳng
định nguyên nhân nếu log không đủ bằng chứng. Kết luận chưa đủ căn cứ phải là
`UNKNOWN`/`REVIEW_REQUIRED`, không suy diễn thành product defect.

## 2. Hiện trạng repository

Đã có MVP gồm input JSON mẫu, adapter từ thư mục run, Gemini provider, CLI, JSON/HTML
output và unit test. Agent **chưa** được đăng ký vào supervisor; adapter chưa nhận kết quả
runner có cấu trúc đáng tin cậy, và chưa suy luận flaky từ nhiều attempt có thể so sánh.

Các hợp đồng hiện có cần tương thích:

- `SharedState.test_results` chứa kết quả theo run, hiện chỉ gồm `status` (`PASS`/`FAIL`)
  và `log_tail`.
- Executor trong workflow chạy pytest và chỉ trả status cùng tối đa 2.000 ký tự cuối log.
- Test Case Generator sinh test case với `test_case_id`, `type`, `module`, `steps`,
  `expected_result`, `trace_scenario_ids` và `trace_rule_ids`.
- Risk Prioritization giữ test case gốc, ID, mức ưu tiên, thứ tự và các trường truy vết.
- Orchestrator riêng có kết quả chi tiết hơn: `test_case_id`, `state`, `attempt`,
  thời lượng, `error_message`, `blocked_by` và `evidence_uris`.
- Requirement Analyzer hiện cũng nhận log của lần chạy mới nhất và ghi `defects`.
  Hai agent sẽ bị chồng lấn nếu không phân định vai trò.

## 3. Hợp đồng đầu vào

Hợp đồng chuẩn là một đối tượng JSON chứa `tests`, mỗi phần tử đại diện một test case trong
một attempt. File mẫu có một API assertion mismatch và một lỗi khởi chạy Playwright, nằm tại
`examples/sample_failure_input.json`. Giữ cùng schema khi thay bằng dữ liệu chạy thật;
không gộp evidence giữa attempt.

| Nhóm | Trường | Bắt buộc | Ý nghĩa / quy tắc |
|---|---|---|---|
| Identity | `schema_version`, `project_id`, `run_id`, `attempt_id`, `test_case_id` | Có | Phân biệt hợp đồng, dự án, lần chạy, lần retry và đơn vị phân tích. Không dùng `retry_count` thay `attempt_id`. |
| Version | `app_build_id` hoặc commit, `test_suite_version`, `requirement_version`, `test_plan_version` | Có nếu nguồn có version | Ngăn so sánh expected từ phiên bản khác với build/test suite đang chạy. Ghi `null` và cảnh báo nếu không truy xuất được. |
| Agent 1 context | `rule_ids`, `scenario_ids`, `scope_decision`, `source_refs`, `open_issues` | Nên có | Liên kết Requirement Analyzer: xác minh expected có nguồn và test có thuộc scope; giữ nguyên issue chưa đóng. |
| Agent 2 context | `title`, `preconditions`, `steps`, `test_data_refs`, `expected`, `expected_review_status` | Nên có | Mô tả test case đã thực thi. `expected_review_status` giúp không xem expected chưa duyệt là chuẩn xác tuyệt đối. |
| Agent 3 context | `risk_score`, `risk_reason`, `priority`, `risk_model_version` | Không | Hỗ trợ xếp thứ tự triage; không dùng để suy ra failure category hoặc severity. |
| Execution | `runner`, `runner_version`, `status`, `failed_step`, `error_type`, `error_message`, `started_at`, `ended_at` | Có trong production | Định vị điểm lỗi và bối cảnh. Giữ status gốc; chuẩn hóa status chỉ ở adapter có ghi nhận mapping. |
| Actual | `assertion_values`, `filtered_response`, `ui_state`, `db_state` | Theo loại test | Quan sát để so sánh expected/actual. Response phải được lọc secret trước khi lưu hoặc gửi đến model. |
| Environment | `base_url`, `browser`, `os`, `locale`, `timezone`, `health_snapshot`, `dependency_mode` | Nên có | `base_url` phải thuộc target được phép. Dùng để phân biệt cấu hình môi trường với hành vi sản phẩm. |
| Evidence manifest | `evidence_id`, `kind`, `artifact_ref`, `sha256`, `captured_at`, `run_id`, `attempt_id`, `build_id`, `redaction_status` | Khi có evidence | Truy xuất đúng artifact, xác minh toàn vẹn và đảm bảo evidence thuộc đúng run/attempt/build. |
| Retry history | Kết quả từng attempt và điều kiện thay đổi | Nên có | Phân biệt lặp lại cùng điều kiện với retry sau khi thay đổi build, dữ liệu hoặc môi trường. Không gộp evidence của attempts. |
| Permissions | `allowed_tools`, `retry_budget`, `target_environment`, `replay_policy` | Có trước khi đề xuất thao tác | Giới hạn các hành động Agent được phép đề xuất/yêu cầu; đây không phải quyền tự thực thi. |

### 3.1 Hợp đồng legacy và validation

`--run-dir` hỗ trợ thư mục `evidence/<test-id>/attempt_<n>/`, ghép ngữ cảnh từ
`risk_prioritization/outputs/prioritized_tests.json`,
`test_case_generator/outputs/comic/test_cases/test_cases.json` và
`requirement_analyzer/outputs/comic/analysis.json` bằng **exact test/rule/scenario ID**.
Adapter tạo `prepared_input.json`, SHA-256 manifest và metadata trace an toàn (loại lỗi,
method, timeout, nhóm selector); không gửi trace thô, screenshot, cookie, header, URL, DOM
hoặc network payload cho Gemini. Giá trị test data nhạy cảm, email, số điện thoại và URL
được lọc khỏi ngữ cảnh văn bản.

Run được dùng trong lần phân tích gần nhất là `20261106-101236`: 59 test IDs, trong đó
45 khớp exact ID với nguồn test/risk và 14 không khớp. Nguồn test/risk được tạo ngày
2026-10-30, nguồn requirement ngày 2026-10-06; không có build/version chạy để xác minh
chúng còn đúng với run ngày 2026-11-06. Chỉ 26/45 test khớp ít nhất một rule/scenario hiện
có. Adapter ghi rõ các giới hạn này trong provenance/warnings, không fuzzy-join và không
xem expected chưa duyệt là chuẩn đã xác nhận.

Run có file kết quả authoritative tại
`risk_prioritization/outputs/execution_results_<run-id>.json`. Adapter kiểm tra `run_id`,
ghép từng dòng bằng `test_case_id` + `attempt_id`, rồi dùng trạng thái, expected/actual,
observed facts, thông tin browser và evidence refs của đúng run. Với run
`20261106-101236`, file này có 64 dòng (29 PASS, 30 FAIL, 5 MANUAL); 59 dòng có thư mục
evidence. Nếu không tìm thấy file execution results thì adapter mới quay về suy luận từ
screenshot; không được coi thiếu screenshot là PASS.

Các actual/result strings có thể chứa URL, nội dung DOM hoặc thông tin nhạy cảm. Adapter
chỉ trích reason và một số trường UI đã lọc, không chuyển raw actual, URL hay DOM đầy đủ
sang Gemini. Các dấu hiệu như `target not found` và `Expected pattern not found` được phân
loại `TEST` theo lỗi automation/assertion; missing validation feedback có thể được gán
`PRODUCT` nếu record cùng run ghi nhận rõ dấu hiệu đó. Timeout đơn lẻ không quyết định
category. Nếu bằng chứng không đủ, giữ `UNKNOWN`/`NEEDS_EVIDENCE`.

### 3.2 Nguồn context và bảo vệ dữ liệu

- Risk score/priority chỉ là thông tin triage; không dùng để suy ra loại failure hoặc severity.
- Rule/scenario được đưa vào context chỉ khi IDs khớp chính xác với test case.
- `expected_review_status` mặc định `UNVERIFIED` nếu nguồn không cung cấp trạng thái review.
- Gemini chỉ nhận các trường đã lọc cùng danh sách evidence refs được phép; refs model trả về
  được kiểm tra để ngăn citation giả.
- Evidence manifest giữ hash và tham chiếu local để truy vết. Đường dẫn artifact không gửi
  cho Gemini và raw evidence không được tải lên.

Trước khi phân tích:

1. Xác minh identity của input khớp run, attempt và test cần phân tích; không lấy log/evidence
   từ attempt khác để lấp chỗ trống.
2. Xác minh evidence manifest có cùng `run_id` và `attempt_id`. MVP giữ `artifact_ref` nhưng
   chưa truy cập artifact hoặc xác minh hash.
3. Nếu test status là pass, trả trạng thái phù hợp mà không gán nhãn failure.
4. Nếu expected chưa review, thiếu actual hoặc log bị cắt, đưa hạn chế vào `missing_evidence`
   và giảm mức kết luận; không biến thiếu dữ liệu thành lỗi sản phẩm.
5. Từ chối input sai schema/identity hoặc báo `ERROR`; không thay bằng dữ liệu rỗng hay ghép
   test ID theo tên gần giống.
6. Chỉ truy cập artifact/tool/environment nếu được cho phép bởi `allowed_tools`,
   `target_environment` và `replay_policy`. Mặc định phân tích là read-only.

## 4. Hợp đồng đầu ra

Đầu ra JSON có envelope `schema_version`, `project_id`, `run_id` và mảng `analyses`
(một phần tử cho mỗi test/attempt). Tách rõ facts quan sát được, giả thuyết, yếu tố góp
phần và hành động đề xuất. `evidence_refs` có thể trỏ tới field input (`input:execution`,
`input:actual.assertion_values`, ...) hoặc `evidence_id` trong manifest.

```json
{
  "analysis_id": "fa-<unique-id>",
  "schema_version": "1.0",
  "project_id": "project-123",
  "run_id": "run-123",
  "attempt_id": "attempt-1",
  "test_case_id": "TC-001",
  "analysis_status": "COMPLETED",
  "primary_category": "PRODUCT",
  "subtype": "product.assertion_mismatch",
  "summary": "Assertion quan sát được khác expected đã review; kết luận giới hạn ở test này.",
  "observations": [
    {
      "observation_id": "obs-1",
      "fact": "Response status thực tế là 500.",
      "evidence_refs": ["input:actual.assertion_values"]
    }
  ],
  "hypotheses": [
    {
      "hypothesis_id": "hyp-1",
      "explanation": "Build xử lý request không đúng trong điều kiện đã ghi nhận.",
      "supporting_evidence_refs": ["input:actual.assertion_values"],
      "contradicting_evidence_refs": [],
      "assessment": "PLAUSIBLE"
    }
  ],
  "confidence_score": 0.72,
  "confidence_basis": "Expected đã review và actual được ghi nhận; chưa có lần chạy đối chứng.",
  "missing_evidence": ["Kết quả replay trên cùng build và cùng dữ liệu."],
  "contributing_factors": [],
  "next_actions": [
    {
      "action": "Đối chiếu server log cùng correlation ID.",
      "owner_role": "DEVELOPER",
      "requires_approval": true
    }
  ],
  "routing": "BUG_REPORT_DRAFT",
  "severity_suggestion": {
    "level": "MEDIUM",
    "reason": "Một test thất bại; chưa có dữ liệu về phạm vi ảnh hưởng."
  },
  "review_status": "REVIEW_REQUIRED",
  "provenance": {
    "model": "model-id",
    "prompt_version": "failure-analysis-v1",
    "policy_version": "failure-policy-v1",
    "input_fingerprint": "sha256:<digest>",
    "created_at": "2026-11-06T03:03:00Z"
  }
}
```

### 4.1 Giá trị chuẩn và quy tắc

- `analysis_id` định danh chẩn đoán, không thay thế `test_case_id`.
- `analysis_status`: `COMPLETED`, `NEEDS_EVIDENCE`, `ERROR`. `ERROR` là lỗi xử lý/agent,
  khác với trạng thái test thất bại. Thiếu dữ liệu quan trọng để phân biệt giả thuyết dùng
  `NEEDS_EVIDENCE`; lỗi validation/provider dùng `ERROR`.
- `primary_category` chỉ nhận `PRODUCT`, `TEST`, `DATA`, `ENVIRONMENT`, `UNKNOWN`.
  `subtype` phải có namespace theo policy (ví dụ `product.assertion_mismatch`,
  `test.stale_expected`, `data.invalid_fixture`, `environment.dependency_unavailable`);
  không tự phát sinh taxonomy không được policy cho phép.
- Với test `PASS`, `failure_present` là `false` và `primary_category` là `null` vì không có
  failure để phân loại. Với test lỗi, `failure_present` là `true`; chỉ lúc đó `UNKNOWN`
  mới biểu thị chưa xác định được loại lỗi.
- Dùng `UNKNOWN` khi bằng chứng chưa đủ chọn category. Không ép chọn một trong bốn nhóm
  còn lại và không xem UNKNOWN là lỗi agent.
- `summary` ngắn, nêu quan sát và giới hạn kết luận.
- `observations` chỉ chứa facts; `hypotheses` chứa diễn giải, bằng chứng ủng hộ, phản chứng
  và đánh giá. Không đặt giả thuyết dưới dạng fact.
- `confidence_score` là số 0–1 hoặc `null`. Đây là tự đánh giá nếu chưa hiệu chuẩn, không
  phải accuracy; `confidence_basis` phải nêu căn cứ và bất định.
- `missing_evidence` liệt kê dữ liệu cụ thể có thể phân biệt các giả thuyết.
- `contributing_factors` cho phép nhiều yếu tố đồng thời; không buộc mọi failure chỉ có
  một nguyên nhân.
- `next_actions` là đề xuất có `owner_role` và `requires_approval`, không phải lệnh tự chạy.
- `routing`: `BUG_REPORT_DRAFT`, `TEST_MAINTENANCE`, `DATA_FIX`, `ENV_CHECK`,
  `MANUAL_REVIEW`.
- `severity_suggestion` là đề xuất và lý do hoặc `null`; không sao chép risk score/priority
  thành severity.
- `review_status` mặc định `REVIEW_REQUIRED`. `provenance` ghi model, prompt/policy version,
  input fingerprint và thời điểm tạo để tái lập/kiểm tra.
- `input:...` là pointer tới dữ liệu đầu vào; mọi ref khác phải khớp `evidence_id` trong
  manifest. Không tạo bằng chứng, expected hay actual. Evidence thuộc attempt khác không
  được dùng làm căn cứ cho attempt hiện tại.

### 4.1.1 Quy tắc phân loại của MVP

- `PRODUCT`: chỉ khi assertion được ghi rõ là fail, có cả expected/actual, và expected có
  review status `APPROVED`, `REVIEWED` hoặc `ACCEPTED`.
- `TEST`, `DATA`, `ENVIRONMENT`: khi `error_type` chỉ rõ namespace tương ứng hoặc có một
  số dấu hiệu runner/setup được nhận diện trong `error_type`/`error_message`.
- `UNKNOWN`: khi không có bằng chứng đủ phân biệt category; trạng thái là
  `NEEDS_EVIDENCE`.
- `risk_score`/`priority` không tham gia phân loại; severity hiện để `null`.
- Confidence là heuristic chưa hiệu chuẩn; luôn yêu cầu review.

Với run có execution results, Gemini nhận test context đã ghép bằng ID chính xác, reason/
UI observation đã lọc và trace signatures an toàn. Không gửi đường dẫn nội bộ, screenshot,
trace thô, URL, DOM đầy đủ, request/response hoặc secret. Nhãn của Gemini chỉ được nhận
khi hypothesis dẫn chiếu bằng chứng runtime của chính record; nếu thiếu căn cứ, nhãn vẫn
là `UNKNOWN`.

Các lệnh dưới đây chạy từ thư mục gốc repository trong PowerShell. Dùng Python của virtual
environment dự án để tránh lệch dependency.

Sinh lại hai file mẫu bằng rules local (không cần Gemini API key):

```powershell
& ".\.venv\Scripts\python.exe" -m AI.app.agents.failure_analyzer.main `
  --input "AI\app\agents\failure_analyzer\examples\sample_failure_input.json" `
  --output "AI\app\agents\failure_analyzer\outputs\sample_run"
```

Lệnh trên tạo/ghi đè:

- `AI\app\agents\failure_analyzer\outputs\sample_run\failure_analysis.json`
- `AI\app\agents\failure_analyzer\outputs\sample_run\failure_analysis.html`

Chạy một thư mục run đã lưu bằng Gemini:

```powershell
& ".\.venv\Scripts\python.exe" -m AI.app.agents.failure_analyzer.main `
  --run-dir "AI\app\storage\runs\<run-id>" `
  --project-id "comic-store" `
  --provider gemini `
  --model "gemini-flash-lite-latest" `
  --output "AI\app\storage\runs\<run-id>\failure_analysis"
```

Lệnh Gemini tạo `prepared_input.json` cùng hai report JSON/HTML trong thư mục `--output`.
Request được chia tối đa 10 test mỗi batch; lỗi dịch vụ tạm thời `502/503/504` được thử lại
thêm tối đa hai lần với thời gian chờ tăng dần. Lỗi khác (ví dụ key/model không hợp lệ hoặc
quota) không tự retry. `gemini-flash-lite-latest` đã được xác nhận hoạt động với API key
hiện tại vào ngày 2026-11-06; model availability có thể thay đổi theo thời gian.
Đặt `GEMINI_API_KEY` trong `AI/.env` (đã được `.gitignore` loại trừ) hoặc
`AI/app/agents/failure_analyzer/.env`; CLI đọc các file này nhưng không in giá trị key.
Không ghi key vào file input, source code hoặc commit và không gửi key qua chat.

Hoặc thay JSON mẫu bằng input đã chuẩn hóa:

```powershell
& ".\.venv\Scripts\python.exe" -m AI.app.agents.failure_analyzer.main `
  --input "path\to\failure_input.json" `
  --provider gemini `
  --output "path\to\output"
```

Khi không có key, CLI dừng với lỗi rõ ràng sau khi ghi `prepared_input.json`; không tạo
Gemini analysis giả.

### 4.2 Tương thích với supervisor hiện tại

Node có thể trả:

```python
{
    "failure_analysis": {...},  # kết quả chuẩn, có cấu trúc
    "defects": "...",           # summary tương thích tạm thời với SharedState
    "trace": ["failure_analyzer: ..."]
}
```

`defects` chỉ là trường tương thích legacy; không phải nguồn chuẩn và không được làm mất
`failure_analysis` khi checkpoint. `analysis_status == "ERROR"` phải được supervisor
xử lý như lỗi agent, không như diagnosis hợp lệ hoặc bằng chứng test defect.

### 4.3 File báo cáo

Khi có `run_dir` hoặc output directory, ghi:

| Tệp | Nội dung |
|---|---|
| `failure_analysis.json` | Kết quả có cấu trúc, schema version, provenance và truy vết evidence. |
| `failure_analysis.html` | Bảng tóm tắt test/attempt/status/category/subtype/summary. JSON là output đầy đủ cho observations, hypotheses và next actions. |

Báo cáo phải escape nội dung log khi render HTML, phân biệt dữ liệu quan sát được với
nhận định, và không nhúng nội dung nhạy cảm ngoài những gì cần thiết để debug.

## 5. Ranh giới với các agent khác

- **Executor / Risk Orchestrator:** chạy test và cung cấp kết quả/evidence; Failure Analyzer
  chỉ đọc và giải thích, không điều khiển browser, pytest hay Newman.
- **Requirement Analyzer:** tiếp tục sở hữu phân tích SRS và/hoặc xác minh một sai lệch
  đã được Failure Analyzer chỉ ra có mâu thuẫn với requirement hay không.
- **Failure Analyzer:** sở hữu phân loại kết quả test và chẩn đoán dựa trên log/evidence.
  Không sinh lại rule, scenario hoặc test case.
- **Supervisor:** sở hữu quyết định retry/end. Analyzer chỉ khuyến nghị; không tự tăng
  `retry_count` và không tự gọi executor.
- **Risk Prioritization:** cung cấp priority để sắp xếp điều tra; không phải bằng chứng
  nguyên nhân hay mức độ nghiêm trọng của defect.

## 6. Phần cần bổ sung để chạy production

1. **Adapter dữ liệu:** nối `prioritized_tests.json` + kết quả từng test của orchestrator
   với evidence metadata; bổ sung identity/version, expected review status và actual. Không
   lấy trạng thái từ HTML hoặc aggregate-only JSON.
2. **Execution output:** ghi JSON kết quả từng test/attempt, bao gồm error type/message,
   timestamps, actual có cấu trúc, runner/version và evidence IDs. Khắc phục retry trước khi
   dựa vào lịch sử attempt để đánh giá tính ổn định; hiện MVP chưa phân loại flaky.
3. **Evidence:** nối metadata manifest với từng attempt, kiểm tra SHA-256 và redaction
   status; hiện adapter chỉ ghi artifact reference, chưa tính hash hoặc kiểm tra redaction.
4. **Policy:** hiệu chuẩn subtype, confidence và severity với người review; các giá trị MVP
   chỉ là heuristic, không dùng tự động để đóng bug hoặc quyết định phát hành.
5. **Supervisor integration:** đăng ký node trong `build_graph`, route khi có run failure,
   lưu `failure_analysis` trong checkpoint và tránh phân tích lặp cùng run. Chốt ranh giới
   với Requirement Analyzer để không ghi đè/trùng `defects`.
6. **Mở rộng kiểm thử:** adapter risk/execution thật, version mismatch, retry history,
   evidence integrity và tích hợp supervisor.

## 7. Tiêu chí nghiệm thu tối thiểu

- Không nhầm run hiện tại với run cũ; không phân tích PASS như failure.
- Mọi test được chẩn đoán phải truy được về ID từ input; mọi quote phải có trong evidence.
- Assertion mismatch chỉ được gán `PRODUCT` khi expected đã review và actual được ghi rõ;
  trường hợp khác không được suy diễn category.
- Output giữ nguyên run/attempt/test identity; input sai manifest identity bị từ chối.
- JSON/HTML output được sinh từ input schema; HTML escape nội dung đầu vào.
- Input chưa đủ bằng chứng trả `NEEDS_EVIDENCE`; tất cả chẩn đoán giữ
  `REVIEW_REQUIRED`.
- MVP standalone không tự chạy test, truy cập evidence artifact, retry hoặc thay đổi SRS.
- Agent không tự chạy lại test, thay đổi retry counter, sửa test case hoặc thay đổi SRS.
