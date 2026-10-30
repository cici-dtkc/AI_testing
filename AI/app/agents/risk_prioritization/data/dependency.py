"""Step 5: dependency graph, effective score, topological order (Kahn + priority queue)."""
from __future__ import annotations

import heapq
import re


def natural_key(s: str) -> tuple:
    return tuple(int(p) if p.isdigit() else p for p in re.split(r"(\d+)", s))


def clean_dependencies(ids: list[str], raw: dict[str, list[str]]) -> tuple[dict[str, list[str]], list[str]]:
    known, warns, out = set(ids), [], {}
    for tc, pres in raw.items():
        if tc not in known:
            continue
        keep = []
        for p in pres:
            if p == tc:
                warns.append(f"{tc}: self-dependency ignored")
            elif p not in known:
                warns.append(f"{tc}: unknown prerequisite {p} ignored")
            elif p not in keep:
                keep.append(p)
        if keep:
            out[tc] = keep
    return out, warns


def transitive_dependents(ids: list[str], prereqs: dict[str, list[str]]) -> dict[str, set[str]]:
    direct: dict[str, set[str]] = {i: set() for i in ids}
    for tc, pres in prereqs.items():
        for p in pres:
            direct[p].add(tc)
    result = {}
    for i in ids:
        seen, stack = set(), list(direct[i])
        while stack:
            n = stack.pop()
            if n in seen or n == i:
                continue
            seen.add(n)
            stack.extend(direct[n])
        result[i] = seen
    return result


def order(ids, prereqs, S, R, H, dependents) -> tuple[list[str], list[str], dict[str, float]]:
    """Returns (execution order, ids left over by a cycle, S_eff)."""
    s_eff = {i: max([S[i], *[S[v] for v in dependents[i]]]) for i in ids}

    def key(i):
        return (-s_eff[i], -S[i], -R[i], -H[i], natural_key(i))

    waiting = {i: len(prereqs.get(i, [])) for i in ids}
    children: dict[str, list[str]] = {i: [] for i in ids}
    for tc, pres in prereqs.items():
        for p in pres:
            children[p].append(tc)
    heap = [(key(i), i) for i in ids if waiting[i] == 0]
    heapq.heapify(heap)
    out: list[str] = []
    while heap:
        _, i = heapq.heappop(heap)
        out.append(i)
        for c in children[i]:
            waiting[c] -= 1
            if waiting[c] == 0:
                heapq.heappush(heap, (key(c), c))
    left = sorted((i for i in ids if i not in set(out)), key=lambda i: (-S[i], -R[i], -H[i], natural_key(i)))
    return out + left, left, s_eff
