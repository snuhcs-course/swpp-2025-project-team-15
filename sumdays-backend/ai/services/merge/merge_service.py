import numpy as np
from openai import OpenAI
import os
from ..extract.extract_service import embed_sentences

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def build_prompt(style_prompt, style_examples, memos):
    prompt = (
        "당신은 사용자의 문체를 보존하며 일기를 작성하는 보조자입니다.\n\n"
        "=== 문체 요약 ===\n"
        f"{style_prompt}\n\n"
        "=== 대표 문장 예시 ===\n"
    )
    for ex in style_examples:
        prompt += f"- {ex}\n"

    prompt += "\n=== 오늘의 메모들 ===\n"
    for m in memos:
        prompt += f"- {m}\n"

    prompt += (
        "\n=== 작성 지침(중요) ===\n"
        "• 각 메모는 하나의 사건/분위기 단위입니다.\n"
        "• 메모에 없는 내용을 새로 만들지 말 것.\n"
        "• 각 메모에 대한 서술은 원문 길이의 최대 약 1.5배 범위를 넘기지 마세요.\n"
        "• 문장을 숫자 목록(1. 2. 3.)으로 만들지 말 것.\n"
        "• 사용자의 문체대로 작성하세요. 문장은 마침표로 명확히 끝내세요.\n"
        "• 지금부터는 한 번에 '문장 하나'만 제시하세요.\n"
    )
    return prompt



def generate_next_sentence(prompt, context, k=3):
    """
    prompt + 지금까지의 문맥(context)을 기반으로
    다음 문장을 k개 후보로 생성
    """
    messages = [
        {"role": "system", "content": "당신은 사용자의 문체를 보존하는 일기 작성 어시스턴트입니다."},
        {"role": "user", "content": prompt + "\n\n지금까지 작성된 문장:\n" + context + "\n\n다음 문장을 " + str(k) + "가지 버전으로 제시하세요."}
    ]

    response = client.chat.completions.create(
        model=os.getenv("GPT_MODEL", "gpt-4.1-mini"),
        messages=messages,
        n=k,
    )

    return [choice.message.content.strip() for choice in response.choices]


def pick_best_sentence(candidates, style_vector):
    """
    후보 문장들 → embedding → 스타일 벡터와 cosine similarity → 가장 유사한 문장 선택
    """
    cand_vecs = embed_sentences(candidates)  # (k, D)
    style_vec = np.array(style_vector).reshape(1, -1)  # (1, D)

    sims = (cand_vecs @ style_vec.T).reshape(-1)  # (k,)
    best_idx = np.argmax(sims)

    return candidates[best_idx]


def generate_diary(memos, style_vector, style_prompt, style_examples):
    """
    chunk 단위로 자연스럽게 한 문장씩 이어가는 방식의 일기 생성.
    length_limit = 각 메모 길이 * 1.5
    """
    prompt = build_prompt(style_prompt, style_examples, memos)

    context = ""
    memo_idx = 0
    lengths = [0] * len(memos)
    max_lengths = [int(len(m) * 1.5) for m in memos]

    while memo_idx < len(memos):
        candidates = generate_next_sentence(prompt, context, k=3)
        best = pick_best_sentence(candidates, style_vector)
        context += best + "\n"

        lengths[memo_idx] += len(best)

        # 길이 제한 도달 → 다음 메모로 이동
        if lengths[memo_idx] >= max_lengths[memo_idx] or best.endswith((".", "다", "요")):
            memo_idx += 1

    return context.strip()
