"""Step 2: map each TC to requirement (use case), module and source IDs."""
from __future__ import annotations

import re
from collections import Counter
from dataclasses import dataclass, field

from ..config.config import Config

_PRIORITY_RE = re.compile(r"Priority\s*\|\s*([^\n]+)")
_MODULE_IN_ID = re.compile(r"-M(\d{2})(?:-|$)")


@dataclass
class TCContext:
    tc_id: str
    module: str
    use_cases: list[str] = field(default_factory=list)
    uc_priorities: list[str] = field(default_factory=list)
    source_ids: list[str] = field(default_factory=list)
    block_ids: list[str] = field(default_factory=list)
    original_priority: str | None = None
    runner: str = "ui"
    depends_on: list[str] = field(default_factory=list)


class AnalysisIndex:
    """Lookup rule_id -> blocks (module, use case, priority) from analysis.json."""

    def __init__(self, analysis: dict | None):
        self.blocks, self.rules = {}, {}
        if analysis:
            for b in analysis.get("source_document", {}).get("blocks", []):
                self.blocks[b["block_id"]] = b
            for r in analysis.get("analysis", {}).get("rules", []):
                self.rules[r["rule_id"]] = r

    def lookup(self, rule_ids: list[str]) -> dict:
        modules, ucs, prios, blocks, nfr = [], [], [], [], False
        for rid in rule_ids:
            rule = self.rules.get(rid)
            if not rule:
                continue
            nfr = nfr or rule.get("category") == "NON_FUNCTIONAL"
            for ref in rule.get("source_refs", []):
                blocks.append(ref["block_id"])
                b = self.blocks.get(ref["block_id"])
                if not b:
                    continue
                if b.get("module"):
                    modules.append(b["module"])
                if b.get("use_case"):
                    ucs.append(b["use_case"])
                    m = _PRIORITY_RE.search(b["text"])
                    if m:
                        prios.append(m.group(1).strip())
        return {"modules": modules, "use_cases": ucs, "priorities": prios, "blocks": blocks, "nfr": nfr}


def build_context(tc: dict, index: AnalysisIndex, cfg: Config, extra_deps: dict | None) -> TCContext:
    rule_ids = tc.get("trace_rule_ids", [])
    scen_ids = tc.get("trace_scenario_ids", [])
    info = index.lookup(rule_ids)
    modules = list(info["modules"])
    if not modules:                                   # fallback: module code inside the rule id
        modules = [f"M{m}" for rid in rule_ids for m in _MODULE_IN_ID.findall(rid)]
    if modules:
        module = Counter(modules).most_common(1)[0][0]
    else:
        module = "NFR" if info["nfr"] or tc.get("type") == "NON_FUNCTIONAL" else "UNMAPPED"

    text = " ".join([tc.get("title", ""), *tc.get("steps", [])]).lower()
    runner = "api" if any(k in text for k in cfg.api_keywords) else "ui"

    deps = list(tc.get("depends_on", []))
    own_id = tc["test_case_id"]
    for pre in tc.get("preconditions", []):          # TC IDs written in pre-conditions
        deps += [x for x in re.findall(r"\bTC-\d+\b", pre) if x != own_id]
    deps += (extra_deps or {}).get(own_id, [])

    return TCContext(
        tc_id=own_id, module=module,
        use_cases=list(dict.fromkeys(info["use_cases"])),
        uc_priorities=info["priorities"],
        source_ids=list(dict.fromkeys([*scen_ids, *rule_ids])),
        block_ids=list(dict.fromkeys(info["blocks"])),
        original_priority=tc.get("priority"), runner=runner,
        depends_on=list(dict.fromkeys(deps)),
    )
