import json, time
from google import genai
from google.genai import types, errors
from .models import TestSuite
from .prompt import SYSTEM_PROMPT

def gemini_schema(model: type) -> dict:
    """Hàm chuẩn hóa JSON schema cho Gemini API."""
    schema = model.model_json_schema()
    def remove_additional_properties(value):
        if isinstance(value, dict):
            value.pop('additionalProperties', None)
            for child in value.values(): remove_additional_properties(child)
        elif isinstance(value, list):
            for child in value: remove_additional_properties(child)
    remove_additional_properties(schema)
    return schema

def generate_test_cases(analysis_data: dict, key: str, model: str) -> TestSuite:
    if not key or key == 'your_gemini_api_key_here':
        raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
    
    schema = gemini_schema(TestSuite)

    # Tăng timeout lên 300 giây để tránh bị timeout khi generate file lớn
    with genai.Client(api_key=key, http_options=types.HttpOptions(timeout=300000)) as client:
        for attempt in range(3):
            try:
                response = client.models.generate_content(
                    model=model,
                    contents=json.dumps(analysis_data, ensure_ascii=False),
                    config=types.GenerateContentConfig(
                        system_instruction=SYSTEM_PROMPT,
                        temperature=0,
                        response_mime_type='application/json',
                        response_schema=schema,
                        max_output_tokens=65536
                    )
                )
                if not response.text:
                    raise ValueError('Gemini không trả text. Kiểm tra model hoặc input.')
                return TestSuite.model_validate_json(response.text)
            except errors.APIError as exc:
                if exc.code in (429, 500, 502, 503, 504) and attempt < 2:
                    time.sleep(2**attempt); continue
                raise RuntimeError(f'Gemini API lỗi {exc.code}.') from None
    raise RuntimeError('Gemini sinh test case thất bại.')