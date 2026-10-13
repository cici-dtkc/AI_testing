from __future__ import annotations

import sys
import time
from collections.abc import Callable
from typing import TypeVar

from app.state.models import Route

GEMINI_MAX_ATTEMPTS = 4
GEMINI_RETRY_BASE_SECONDS = 2
T = TypeVar("T")


class RealLLM:
    """LLM adapter for Gemini."""

    def __init__(self, model_name: str):
        try:
            from langchain.chat_models import init_chat_model
        except ImportError:
            sys.exit("Thiếu gói: pip install langchain")
        try:
            self.model = init_chat_model(model_name, temperature=0)
        except ImportError as exc:
            sys.exit(
                f"Thiếu gói tích hợp cho '{model_name}': {exc}\n"
                "Gemini: pip install langchain-google-genai"
            )

    def _invoke_with_retry(self, operation: Callable[[], T]) -> T:
        from google.genai.errors import ServerError
        from langchain_core.exceptions import ModelNotFoundError

        for attempt in range(1, GEMINI_MAX_ATTEMPTS + 1):
            try:
                return operation()
            except ModelNotFoundError as exc:
                raise RuntimeError(
                    "Model Gemini không khả dụng cho API key hiện tại. "
                    "Hãy dùng GEMINI_MODEL=gemini-3.5-flash-lite hoặc truyền "
                    "--model google_genai:gemini-3.5-flash-lite."
                ) from exc
            except ServerError:
                if attempt == GEMINI_MAX_ATTEMPTS:
                    raise
                delay = GEMINI_RETRY_BASE_SECONDS * (2 ** (attempt - 1))
                print(
                    f"Gemini tạm thời không khả dụng (503), thử lại sau {delay}s "
                    f"({attempt}/{GEMINI_MAX_ATTEMPTS - 1})...",
                    file=sys.stderr,
                )
                time.sleep(delay)

        raise RuntimeError("Không thể gọi Gemini sau nhiều lần thử.")

    def ask(self, system: str, user: str) -> str:
        response = self._invoke_with_retry(
            lambda: self.model.invoke([("system", system), ("user", user)])
        )
        return response.text

    def route(self, prompt: str) -> Route:
        structured_model = self.model.with_structured_output(
            Route, method="json_schema"
        )
        return self._invoke_with_retry(lambda: structured_model.invoke(prompt))
