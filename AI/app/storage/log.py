 
from __future__ import annotations

import sys
import time
from pathlib import Path

_loggers: dict[str, "RunLogger"] = {}


def safe_print(text: str) -> None:
    """print() that never dies on a non-UTF8 console: falls back to ASCII."""
    try:
        print(text, flush=True)
    except UnicodeEncodeError:
        try:
            print(text.encode("ascii", "replace").decode("ascii"), flush=True)
        except Exception:
            pass


class RunLogger:
    """Append-only logger for one pipeline run."""

    def __init__(self, run_dir: str | Path, echo: bool = True) -> None:
        self.run_dir = Path(run_dir)
        self.path = self.run_dir / "pipeline.log"
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self._fh = open(self.path, "a", encoding="utf-8", buffering=1)  # line-buffered
        self.echo = echo

    #low level  
    def _emit(self, marker: str, msg: str) -> None:
        ts = time.strftime("%H:%M:%S")
        text = f"{ts} {marker} {msg}"
        self._fh.write(text + "\n")
        if self.echo:
            safe_print(text)

    #  semantic events  
    def info(self, msg: str) -> None:
        self._emit("•", msg)

    def dispatch(self, agent: str, reason: str) -> None:
        self._emit("→", f"supervisor → {agent}: {reason}")

    def start(self, agent: str) -> None:
        self._emit("▶", f"{agent} đang chạy ...")

    def done(self, agent: str, summary: str) -> None:
        prefix = f"{agent}: "
        if summary.startswith(prefix):
            summary = summary[len(prefix):]
        self._emit("✔", f"{agent}: {summary}")

    def finish(self, reason: str) -> None:
        self._emit("■", f"workflow kết thúc — {reason}")

    def artifact(self, label: str, path: str | Path) -> None:
        self._emit(" ", f"  {label}: {path}")

    def close(self) -> None:
        try:
            self._fh.close()
        except Exception:
            pass


def get_logger(run_dir: str | Path | None, echo: bool = True) -> RunLogger | None:
    """Return the (cached) logger for ``run_dir``, or ``None`` if absent."""
    if not run_dir:
        return None
    key = str(Path(run_dir))
    lg = _loggers.get(key)
    if lg is None or lg._fh.closed:
        lg = RunLogger(run_dir, echo=echo)
        _loggers[key] = lg
    return lg
