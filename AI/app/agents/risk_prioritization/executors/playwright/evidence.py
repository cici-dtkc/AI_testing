from pathlib import Path

def capture_screenshot(page, output_path: str) -> str:
    path = Path(output_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    page.screenshot(path=str(path), full_page=True)
    return str(path)

def start_tracing(context, trace_path: str) -> None:
    path = Path(trace_path)
    path.parent.mkdir(parents=True, exist_ok=True)
    context.tracing.start(screenshots=True, snapshots=True, sources=True)

def stop_tracing(context, trace_path: str) -> str:
    path = Path(trace_path)
    context.tracing.stop(path=str(path))
    return str(path)

def capture_video(page) -> str | None:
    video = page.video
    if video:
        return video.path()
    return None
