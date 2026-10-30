from __future__ import annotations

from collections import defaultdict, deque

from .models import ExecutionPlan


def build_execution_plan(prioritization_result: dict, max_workers: int = 2) -> ExecutionPlan:
    items = prioritization_result["items"]

    dependency_graph = {}
    in_degree = {}

    for item in items:
        tc_id = item["test_case_id"]
        depends_on = item.get("depends_on", [])
        dependency_graph[tc_id] = depends_on
        in_degree[tc_id] = len(depends_on)

    levels = []
    current_level = deque([tc_id for tc_id, deg in in_degree.items() if deg == 0])

    while current_level:
        level_group = []
        for _ in range(len(current_level)):
            tc_id = current_level.popleft()
            level_group.append(tc_id)

            for other_tc, deps in dependency_graph.items():
                if tc_id in deps:
                    in_degree[other_tc] -= 1
                    if in_degree[other_tc] == 0:
                        current_level.append(other_tc)

        level_group.sort(key=lambda tc: next(
            item["execution_order"] for item in items if item["test_case_id"] == tc
        ))
        levels.append(level_group)

    return ExecutionPlan(groups=levels, max_workers=max_workers)
