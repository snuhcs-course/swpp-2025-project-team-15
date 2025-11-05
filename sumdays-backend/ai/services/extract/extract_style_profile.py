import os
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

STYLE_PROFILE_PROMPT = """
다음 글들을 읽고, 글쓴이의 문체적 특징을 정밀하게 분석하세요.
'무슨 내용인지'가 아니라 '어떻게 말하는지'에 집중하세요.

출력 형식(JSON):
{
  "tone": "글 전반의 말투/분위기",
  "formality": "격식체 / 반말 / 구어체 / 문어체 / 혼합",
  "sentence_length": "짧음 / 중간 / 김",
  "sentence_structure": "단문 위주 / 중문+접속사 위주 / 묘사 강조형 / 여운 남김 / 파편적 서술 등",
  "sentence_endings": ["대표적인 문장 어미/종결 표현"],
  "lexical_choice": "어휘 선택 경향",
  "common_phrases": ["특정 빈도로 반복되는 표현들"],
  "emotional_tone": "감정 표현 강도 + 방향",
  "irony_or_sarcasm": "반어/해학/냉소 사용 여부",
  "slang_or_dialect": "비속어 / 인터넷체 / 방언 사용 여부",
  "pacing": "문장 리듬 (빠름 / 보통 / 느림)",
  "overall_style_summary": "위 요소들을 종합 요약"
}

글:
{TEXT}
"""

def compute_style_profile_text(diaries):
    text = "\n".join(diaries)
    prompt = STYLE_PROFILE_PROMPT.replace("{TEXT}", text)

    response = client.chat.completions.create(
        model=os.getenv("GPT_MODEL", "gpt-4.1-nano"),
        messages=[
            {"role": "system", "content": "당신은 일기에 특화된 문체 분석 AI입니다."},
            {"role": "user", "content": prompt}
        ]
    )

    content = response.choices[0].message.content
    return content
