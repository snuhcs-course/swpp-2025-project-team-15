import os
import numpy as np
from openai import OpenAI
from sentence_transformers import SentenceTransformer
from .style_prompt import build_style_prompt
from .style_bias import make_logit_bias

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

ko_embed = SentenceTransformer("jhgan/ko-sroberta-multitask")

def embed(text):
    return ko_embed.encode(text, convert_to_numpy=True)

def cosine(a, b):
    return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-12))

def merge_with_style(contents, style_vector, style_profile, style_examples):

    style_prompt = build_style_prompt(style_profile)
    logit_bias = make_logit_bias(style_profile)

    # ✅ 일기 생성 지시문 (전체를 자연스럽게 작성)
    merge_instruction = (
        "아래 메모들을 참고하여 자연스럽고 부드러운 흐름을 가진 일기를 작성해줘.\n"
        "문장 길이, 말투, 어미 사용은 예문과 동일하게 유지해줘.\n"
        "결과는 하나의 완성된 일기 형태여야 하며, 목록 형식이면 안 된다. 문단은 필요하면 나눠도 된다.\n"
    )

    base_input = merge_instruction + "\n".join(f"- {c}" for c in contents)

    messages = [
        {"role": "system", "content": style_prompt + "\n\n[사용자 문체 예문]\n" + "\n\n".join(style_examples)},
        {"role": "user", "content": base_input}
    ]

    # n 후보 생성
    res = client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=messages,
        n=4,                 # 4개 후보에서 선택
        temperature=0.9,
        max_tokens=500,
        logit_bias=logit_bias
    )

    candidates = [c.message.content.strip() for c in res.choices]

    # style_vector 와 가장 비슷한 후보 선택
    style_vec = np.array(style_vector, dtype=float)
    sims = [cosine(embed(c), style_vec) for c in candidates]
    best = candidates[int(np.argmax(sims))]

    return best
