import json
from pathlib import Path

def save_request_log(request: dict, output_path: str) -> str:
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(request, f, indent=2, ensure_ascii=False)
    return str(path)

def save_response_log(response: dict, output_path: str) -> str:
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(response, f, indent=2, ensure_ascii=False)
    return str(path)
