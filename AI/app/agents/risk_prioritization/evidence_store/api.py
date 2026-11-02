import threading
from datetime import datetime

from .metadata import MetadataIndex
from .retention import soft_delete
from .store import BASE_DIR, save_artifact, sha256_of

_lock = threading.Lock()


def store_evidence(run_id: str, tc_id: str, attempt: int, artifact_type: str, source_path: str) -> str:
    uri = save_artifact(run_id, tc_id, attempt, artifact_type, source_path)
    with _lock:
        index = MetadataIndex.load(run_id)
        index.add_entry(run_id, tc_id, attempt, artifact_type, uri, sha256_of(uri), datetime.now().isoformat())
        index.save()
    return uri


def get_evidence(run_id: str, tc_id: str) -> list[dict]:
    return MetadataIndex.load(run_id).get_by_tc(run_id, tc_id)


def cleanup(run_id: str) -> None:
    path = BASE_DIR / run_id
    if path.is_dir():
        soft_delete(path)
