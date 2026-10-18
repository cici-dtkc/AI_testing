import json,time
from google import genai
from google.genai import types,errors
from .models import Analysis,Document
from .prompt import SYSTEM_PROMPT

def generate(document:Document,key:str,model:str,target_block_ids:list[str]|None=None)->Analysis:
    if not key or key=='your_gemini_api_key_here':raise ValueError('Điền GEMINI_API_KEY trong .env trước khi chạy.')
    with genai.Client(api_key=key,http_options=types.HttpOptions(timeout=90000)) as client:
        for attempt in range(3):
            try:
                response=client.models.generate_content(model=model,contents=json.dumps({**document.model_dump(),"target_block_ids":target_block_ids or [b.block_id for b in document.blocks]},ensure_ascii=False),config=types.GenerateContentConfig(system_instruction=SYSTEM_PROMPT,temperature=0,response_mime_type='application/json',response_schema=Analysis,max_output_tokens=16384))
                if not response.text:raise ValueError('Gemini không trả text. Kiểm tra model, input hoặc safety block.')
                return Analysis.model_validate_json(response.text)
            except errors.APIError as exc:
                if exc.code in (429,500,502,503,504) and attempt<2:
                    time.sleep(2**attempt);continue
                # Avoid dumping provider exception/request which might contain sensitive data.
                raise RuntimeError(f'Gemini API lỗi {exc.code}. Kiểm tra key/quota/model; không có kết quả giả thay thế.') from None
    raise RuntimeError('Gemini thất bại.')
