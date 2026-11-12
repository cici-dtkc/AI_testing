# Module Trích xuất và Ghép nối dữ liệu đa nguồn (Data Correlator & Extractor).
# Thực hiện trích xuất và ghép nối thông tin từ các nguồn dữ liệu thực tế:
# 1. execution_results_20261106-101236_actionable.json: Kết quả chạy test automation
# 2. failure_analysis.json: Kết quả phân tích nguyên nhân từ Failure Analysis Agent (Schema v1.0)
# 3. test_cases_report.html / test_cases.json: Thiết kế ca kiểm thử (preconditions, steps, expected)
# 4. analysis.json: Phân tích yêu cầu nghiệp vụ (Business Rules, Scenarios)

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, Dict, List, Optional

from .schemas import (
    EvidenceItem,
    FAAEvidenceItem,
    FAAHypothesisItem,
    FAANextAction,
    FAAObservation,
    FailureAnalysisOutput,
    ObservedFact,
    StepItem,
    SuspectedCause,
    Traceability,
)


class DataExtractor:
    def __init__(
        self,
        execution_path: Optional[Path] = None,
        failure_analysis_path: Optional[Path] = None,
        test_cases_path: Optional[Path] = None,
        test_cases_html_path: Optional[Path] = None,
        requirement_path: Optional[Path] = None,
    ):
        # Thư mục gốc AI/
        cur = Path(__file__).resolve()
        base_dir = None
        for p in cur.parents:
            if p.name == "AI":
                base_dir = p
                break
        if base_dir is None:
            base_dir = cur.parents[4] if len(cur.parents) > 4 else cur.parents[3]

        primary_exec_path = base_dir / "app/agents/risk_prioritization/outputs/execution_results_20261106-101236_actionable.json"
        local_fallback_exec_path = base_dir / "app/agents/bug_report/inputs/execution_results_20261106-101236_actionable.json"
        
        if execution_path:
            self.execution_path = execution_path
        elif local_fallback_exec_path.exists():
            self.execution_path = local_fallback_exec_path
        else:
            self.execution_path = primary_exec_path
        self.failure_analysis_path = failure_analysis_path or (
            base_dir / "app/agents/failure_analyzer/outputs/failure_analysis.json"
        )
        self.test_cases_path = test_cases_path or (
            base_dir / "app/agents/test_case_generator/outputs/comic/test_cases/test_cases.json"
        )
        self.test_cases_html_path = test_cases_html_path or (
            base_dir / "app/agents/test_case_generator/outputs/comic/test_cases/test_cases_report.html"
        )
        self.requirement_path = requirement_path or (
            base_dir / "app/agents/requirement_analyzer/outputs/comic/analysis.json"
        )

        self.execution_data: Dict[str, Any] = {}
        self.failure_analysis_map: Dict[str, Dict[str, Any]] = {}
        self.test_cases_map: Dict[str, Dict[str, Any]] = {}
        self.rules_map: Dict[str, Dict[str, Any]] = {}
        self.scenarios_map: Dict[str, Dict[str, Any]] = {}

        self._load_all()

    def _load_all(self):
        # Nạp dữ liệu từ tất cả các nguồn file thực tế
        # 1. Nạp Execution Results (Test Runner)
        if self.execution_path.exists():
            with open(self.execution_path, "r", encoding="utf-8") as f:
                self.execution_data = json.load(f)

        # 2. Nạp Failure Analysis Output từ failure_analyzer (Schema v1.0)
        if self.failure_analysis_path.exists():
            with open(self.failure_analysis_path, "r", encoding="utf-8") as f:
                fa_data = json.load(f)
                analyses = fa_data.get("analyses", [])
                for a in analyses:
                    tc_id = a.get("test_case_id")
                    if tc_id:
                        self.failure_analysis_map[tc_id] = a

        # 3. Nạp Test Cases từ file JSON
        if self.test_cases_path.exists():
            with open(self.test_cases_path, "r", encoding="utf-8") as f:
                tc_data = json.load(f)
                tcs = tc_data.get("test_suite", {}).get("test_cases", [])
                for tc in tcs:
                    t_id = tc.get("test_case_id")
                    if t_id:
                        self.test_cases_map[t_id] = tc

        # 4. Ghi đè/cập nhật thông tin chuẩn xác nhất từ test_cases_report.html (Table 2)
        if self.test_cases_html_path.exists():
            self._load_test_cases_from_html()

        # 5. Nạp Requirement Analysis (Business Rules & Scenarios)
        if self.requirement_path.exists():
            with open(self.requirement_path, "r", encoding="utf-8") as f:
                req_data = json.load(f)
                analysis = req_data.get("analysis", {})
                for r in analysis.get("rules", []):
                    r_id = r.get("rule_id") or r.get("id")
                    if r_id:
                        self.rules_map[r_id] = r
                for s in analysis.get("scenarios", []):
                    s_id = s.get("scenario_id") or s.get("id")
                    if s_id:
                        self.scenarios_map[s_id] = s

    def _load_test_cases_from_html(self):
        # Trích xuất dữ liệu chi tiết các test case từ Bảng 2 trong test_cases_report.html
        try:
            with open(self.test_cases_html_path, "r", encoding="utf-8") as f:
                html = f.read()

            tables = re.findall(r'<table[^>]*>(.*?)</table>', html, re.DOTALL)
            if len(tables) >= 2:
                t2 = tables[1]
                trs = re.findall(r'<tr[^>]*>(.*?)</tr>', t2, re.DOTALL)
                for tr in trs[1:]:
                    tds = re.findall(r'<td[^>]*>(.*?)</td>', tr, re.DOTALL)
                    if len(tds) >= 12:
                        tc_id = re.sub(r'<[^>]+>', '', tds[0]).strip()
                        flow = re.sub(r'<[^>]+>', '', tds[1]).strip()
                        title = re.sub(r'<[^>]+>', '', tds[2]).strip()
                        tc_type = re.sub(r'<[^>]+>', '', tds[3]).strip()
                        technique = re.sub(r'<[^>]+>', '', tds[4]).strip()
                        priority = re.sub(r'<[^>]+>', '', tds[5]).strip()

                        # Preconditions
                        precond_lis = re.findall(r'<li[^>]*>(.*?)</li>', tds[6], re.DOTALL)
                        if precond_lis:
                            precond = " ".join([re.sub(r'<[^>]+>', '', li).strip() for li in precond_lis if li.strip()])
                        else:
                            precond = re.sub(r'<[^>]+>', '', tds[6]).strip()

                        # Steps
                        steps_lis = re.findall(r'<li[^>]*>(.*?)</li>', tds[8], re.DOTALL)
                        if steps_lis:
                            steps = [re.sub(r'<[^>]+>', '', li).strip() for li in steps_lis if li.strip()]
                        else:
                            raw_s = re.sub(r'<[^>]+>', '', tds[8]).strip()
                            steps = [raw_s] if raw_s else []

                        expected = re.sub(r'<[^>]+>', '', tds[9]).strip()

                        # Rules
                        rules_lis = re.findall(r'<li[^>]*>(.*?)</li>', tds[11], re.DOTALL)
                        rule_ids = [re.sub(r'<[^>]+>', '', r).strip() for r in rules_lis if r.strip()]

                        # Cập nhật vào test_cases_map
                        current_tc = self.test_cases_map.get(tc_id, {})
                        current_tc.update({
                            "test_case_id": tc_id,
                            "flow": flow,
                            "title": title,
                            "type": tc_type,
                            "technique": technique,
                            "priority": priority,
                            "preconditions": precond,
                            "steps": steps,
                            "expected_result": expected,
                            "trace_rule_ids": rule_ids or current_tc.get("trace_rule_ids", []),
                        })
                        self.test_cases_map[tc_id] = current_tc
        except Exception as e:
            print(f"[DataExtractor] Warning: Không thể đọc test_cases_report.html: {e}")

    def get_actionable_executions(self) -> List[Dict[str, Any]]:
        """Trả về danh sách tất cả các ca FAIL/ERROR từ kết quả chạy test"""
        results = self.execution_data.get("execution_results", [])
        return [r for r in results if r.get("status") in ["FAIL", "ERROR"]]

    def get_test_case_details(self, test_case_id: str) -> Dict[str, Any]:
        """Lấy chi tiết thiết kế test case (preconditions, steps, expected, rules)"""
        return self.test_cases_map.get(test_case_id, {})

    def get_failure_analysis(self, test_case_id: str) -> Optional[Dict[str, Any]]:
        """Lấy bản phân tích từ Failure Analysis Agent cho một test case cụ thể"""
        return self.failure_analysis_map.get(test_case_id)

    def build_or_adapt_failure_analysis(
        self,
        exec_item: Dict[str, Any],
        faa_input: Optional[FailureAnalysisOutput] = None
    ) -> FailureAnalysisOutput:

        # Nạp kết quả thực tế từ Failure Analysis Agent (khớp 100% schema_version '1.0' trong failure_analyzer/core/analyzer.py)
        # Nếu chưa có phân tích thì tự động fallback tạo adapter an toàn.
        if faa_input:
            return faa_input

        tc_id = exec_item.get("test_case_id", "TC-UNKNOWN")
        mod = exec_item.get("module", "General")
        run_id = self.execution_data.get("run_id", "20261106-101236")
        attempt_id = f"attempt_{exec_item.get('attempt_id', 1)}"

        # 1. NẾU CÓ DỮ LIỆU THỰC TỪ FAILURE ANALYSIS AGENT (file failure_analysis.json):
        real_fa = self.failure_analysis_map.get(tc_id)
        if real_fa:
            # Observations (khớp FAAObservation trong analyzer.py)
            obs_list: List[FAAObservation] = []
            for o in real_fa.get("observations", []):
                obs_list.append(
                    FAAObservation(
                        observation_id=o.get("observation_id", "obs-1"),
                        fact=o.get("fact", ""),
                        evidence_refs=o.get("evidence_refs", [])
                    )
                )

            # Hypotheses (khớp FAAHypothesisItem trong analyzer.py)
            hypo_list: List[FAAHypothesisItem] = []
            for h in real_fa.get("hypotheses", []):
                hypo_list.append(
                    FAAHypothesisItem(
                        hypothesis_id=h.get("hypothesis_id", "hyp-1"),
                        explanation=h.get("explanation", ""),
                        supporting_evidence_refs=h.get("supporting_evidence_refs", []),
                        contradicting_evidence_refs=h.get("contradicting_evidence_refs", []),
                        assessment=h.get("assessment", "PLAUSIBLE")
                    )
                )

            # Next Actions (khớp FAANextAction trong analyzer.py)
            actions_list: List[FAANextAction] = []
            for a in real_fa.get("next_actions", []):
                if isinstance(a, dict):
                    actions_list.append(
                        FAANextAction(
                            action=a.get("action", ""),
                            owner_role=a.get("owner_role", "DEVELOPER"),
                            requires_approval=a.get("requires_approval", True)
                        )
                    )

            # Evidence Manifest (khớp FAAEvidenceItem trong analyzer.py)
            ev_list: List[FAAEvidenceItem] = []
            for e in real_fa.get("evidence_manifest", []):
                if isinstance(e, dict):
                    ev_list.append(
                        FAAEvidenceItem(
                            evidence_id=e.get("evidence_id", "EV-001"),
                            kind=e.get("kind", "screenshot"),
                            artifact_ref=e.get("artifact_ref", ""),
                            sha256=e.get("sha256", "N/A"),
                            captured_at=e.get("captured_at"),
                            run_id=e.get("run_id"),
                            attempt_id=e.get("attempt_id"),
                            build_id=e.get("build_id"),
                            redaction_status=e.get("redaction_status")
                        )
                    )

            conf_score = real_fa.get("confidence_score") or 0.65

            return FailureAnalysisOutput(
                schema_version=real_fa.get("schema_version", "1.0"),
                analysis_id=real_fa.get("analysis_id", f"fa-{tc_id}"),
                project_id=real_fa.get("project_id", "comic-ecommerce"),
                run_id=real_fa.get("run_id", run_id),
                attempt_id=real_fa.get("attempt_id", attempt_id),
                test_case_id=tc_id,
                execution_status=real_fa.get("execution_status", "FAILED"),
                failure_present=real_fa.get("failure_present", True),
                analysis_status=real_fa.get("analysis_status", "COMPLETED"),
                primary_category=real_fa.get("primary_category", "PRODUCT"),
                subtype=real_fa.get("subtype"),
                summary=real_fa.get("summary", f"Lỗi thực thi kiểm thử tại {tc_id}"),
                observations=obs_list,
                hypotheses=hypo_list,
                confidence_score=conf_score,
                confidence_basis=real_fa.get("confidence_basis"),
                missing_evidence=real_fa.get("missing_evidence", []),
                contributing_factors=real_fa.get("contributing_factors", []),
                next_actions=actions_list,
                routing=real_fa.get("routing", "BUG_REPORT_DRAFT"),
                severity_suggestion=real_fa.get("severity_suggestion"),
                review_status=real_fa.get("review_status", "REVIEW_REQUIRED"),
                provenance=real_fa.get("provenance"),
                evidence_manifest=ev_list,
                gemini_assessment=real_fa.get("gemini_assessment")
            )

        # 2. FALLBACK NẾU CHƯA CÓ TRONG FAILURE ANALYSIS:
        raw_actual = exec_item.get("actual_result", "")
        reason = "Kiểm thử thất bại trên giao diện người dùng"
        if isinstance(raw_actual, str) and "reason" in raw_actual:
            try:
                parsed_act = json.loads(raw_actual)
                reason = parsed_act.get("reason", reason)
            except Exception:
                pass

        obs_list = [
            FAAObservation(
                observation_id="obs-1",
                fact=f"Trạng thái thực thi là {exec_item.get('status', 'FAIL')}: {reason}",
                evidence_refs=["input:execution"]
            )
        ]
        hypo_list = [
            FAAHypothesisItem(
                hypothesis_id="hyp-1",
                explanation=f"Hệ thống tại {mod} có thể chưa xử lý đúng logic nghiệp vụ hoặc thiếu phản hồi ({reason}).",
                supporting_evidence_refs=["input:execution"],
                assessment="PLAUSIBLE"
            )
        ]
        actions_list = [
            FAANextAction(
                action=f"Kiểm tra controller và component giao diện của {mod} liên quan đến: {reason}.",
                owner_role="DEVELOPER"
            )
        ]

        return FailureAnalysisOutput(
            schema_version="1.0",
            analysis_id=f"fa-{tc_id}",
            project_id=self.execution_data.get("project_id", "comic-ecommerce"),
            run_id=run_id,
            attempt_id=attempt_id,
            test_case_id=tc_id,
            execution_status="FAILED",
            failure_present=True,
            analysis_status="COMPLETED",
            primary_category="PRODUCT",
            summary=f"Lỗi thực thi tại {tc_id} ({mod}): {reason}",
            observations=obs_list,
            hypotheses=hypo_list,
            confidence_score=0.85,
            confidence_basis="Đánh giá từ runner log và bằng chứng execution",
            next_actions=actions_list,
            routing="BUG_REPORT_DRAFT",
            review_status="REVIEW_REQUIRED"
        )
