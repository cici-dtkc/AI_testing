import hashlib
import shutil
from datetime import datetime
from pathlib import Path

BASE_DIR = Path("outputs/evidence")


def save_artifact(run_id: str, tc_id: str, attempt: int, artifact_type: str, file_path: str) -> str:
    src = Path(file_path)
    if not src.is_file():
        raise FileNotFoundError(str(src))
    dest_dir = BASE_DIR / run_id / tc_id / f"attempt_{attempt}"
    dest_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%dT%H%M%S%f")
    dest = dest_dir / f"{artifact_type}_{stamp}{src.suffix}"
    shutil.copy2(src, dest)
    return str(dest)


def sha256_of(path: str | Path) -> str:
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()
