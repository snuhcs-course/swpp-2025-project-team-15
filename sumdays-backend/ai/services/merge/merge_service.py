from sentence_transformers import SentenceTransformer
import os, json, re, math
import numpy as np
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])
embed_model = SentenceTransformer("jhgan/ko-sroberta-multitask")

### ---- helper functions ---- ###
def l2norm(x):
    return x / (np.linalg.norm(x, axis=-1, keepdims=True) + 1e-12)

def embed_sentences(sentences):
    E = embed_model.encode(sentences, convert_to_numpy=True, batch_size=64)
    return l2norm(E)

def choose_best_sentence(candidates, style_vec):
    """
    candidates: list[str] (여기서는 '단락' 후보들)
    style_vec: (D,)
    """
    if not candidates:
        return None

    E = embed_sentences(candidates)  # (N, D)
    sims = (E @ style_vec.reshape(-1, 1)).reshape(-1)  # (N,)
    best_idx = int(np.argmax(sims))
    return candidates[best_idx]

def count_sentences(text: str) -> int:
    text = text.strip()
    if not text:
        return 0
    sentences = re.split(r'[.!?]+\s*|\n+', text)
    sentences = [s for s in sentences if s.strip()]
    return len(sentences)

### ---- merge function (메모 단위 후보 생성/선택) ---- ###
def merge_stream_rerank(memos, style_prompt, style_examples, style_vector, num_candidates: int = 1) -> str:
    """
    메모 단위로:
      - 각 메모에 대해 diary-style 단락 후보 num_candidates개 생성
      - style_vector와의 코사인 유사도 기준으로 최적 단락 선택
      - 선택된 단락들을 순서대로 이어붙여 최종 일기를 만든다.

    memos: List[str]  (이미 order 정렬된 memo content 리스트)
    style_prompt: dict or str
    style_examples: List[str] or str
    style_vector: 1D list/np.ndarray (사용자 스타일 벡터, extract_service에서 계산됨)
    num_candidates: 각 메모당 생성할 단락 후보 개수
    """

    # ----- 프롬프트용 텍스트 정리 -----
    if isinstance(style_prompt, dict):
        style_prompt_text = json.dumps(style_prompt, ensure_ascii=False, indent=2)
    else:
        style_prompt_text = str(style_prompt)

    if isinstance(style_examples, list):
        style_examples_list = style_examples
        style_examples_text = "\n".join(f"- {s}" for s in style_examples)
    else:
        style_examples_list = [str(style_examples)]
        style_examples_text = "- " + str(style_examples)

    # style_vector 정규화
    style_vec = np.array(style_vector, dtype=np.float32).reshape(1, -1)
    style_vec = l2norm(style_vec)[0]  # (D,)

    # 참고용: 전체 메모 목록(인덱스 달아서) – 메모별 프롬프트에 넣어줌
    indexed_memos_text = "\n".join(f"{i+1}. {m}" for i, m in enumerate(memos))

    # 최종 결과
    accumulated_diary = ""

    # system 메시지: “메모 내용 > 스타일” 우선순위 못 박기
    system_msg = """
You are a diary-writing assistant.

Your priorities are, in this exact order:
1) FAITHFULLY reflect the concrete events from the memos.
2) Keep the chronological order of the memos.
3) Then, softly adjust tone and style to match the user's profile.

You MUST NOT invent events that are not clearly implied by the memos.
Every paragraph you write must be grounded in the given focus memo.
You are currently writing the diary ONE MEMO AT A TIME, memo by memo.
"""

    for idx, memo in enumerate(memos):
        # ----- 메모별 문장 수 기반 길이 규칙 (1.3 ~ 1.8배) -----
        base_sent = max(1, count_sentences(memo))
        min_sent = max(1, math.ceil(base_sent * 1.3))
        max_sent = max(min_sent, math.ceil(base_sent * 1.8))

        current_diary_block = (
            accumulated_diary if accumulated_diary.strip()
            else "(nothing yet)"
        )

        focus_memo_idx = idx + 1  # 사람 기준 인덱스
        focus_memo = memo

        # ----- user 프롬프트 (이 메모만 확장) -----
        prompt = f"""
You are a diary writing assistant.

You will be given:
- ONE memo: a fragmented note the user wrote today
- style profile: JSON describing the user's writing tone, phrasing preference, pacing, common expressions
- style examples: several representative sentences the user has written before
- the diary text that has already been written for previous memos

Your PRIMARY job for THIS STEP:
- Take the given memo and rewrite/expand it into a short diary-style paragraph.
- Preserve the concrete events and facts from the memo (time, place, actions, feelings).
- Do NOT invent new events that are not clearly implied by the memo.

Your SECONDARY job:
- Softly adjust the tone, rhythm, and sentence endings to match the style profile and examples.
- Style should never override the factual content of the memo.

---

STYLE PROFILE (JSON):
{style_prompt_text}

STYLE EXAMPLES (for tone only, NOT for events):
{style_examples_text}

---

ALL MEMOS (IN ORDER, WITH INDEX):
{indexed_memos_text}

CURRENT DIARY SO FAR (previous memos already processed):
{current_diary_block}

FOCUS MEMO FOR THIS STEP (memo #{focus_memo_idx}):
\"\"\"{focus_memo}\"\"\"

---

LENGTH RULE FOR THIS MEMO:
- The rewritten paragraph for this memo should be between {min_sent} and {max_sent} sentences.
- It should feel concise and natural, not repetitive.
- It should primarily focus on the events and feelings from the FOCUS MEMO.

Now generate {num_candidates} DIFFERENT candidate diary-style paragraphs
that rewrite ONLY this focus memo in a natural diary style.

Output format (IMPORTANT):
- Each candidate paragraph must be separated by a line containing only: ###
- Inside each candidate, just write the sentences, no numbering, no extra commentary.
"""

        completion = client.chat.completions.create(
            model=os.getenv("GPT_MODEL", "gpt-4.1-nano"),
            messages=[
                {"role": "system", "content": system_msg},
                {"role": "user",  "content": prompt}
            ],
            temperature=0.8,
            top_p=0.9,
            max_tokens=512,
        )

        raw = completion.choices[0].message.content or ""
        # "###" 단위로 후보 단락 나누기
        blocks = [blk.strip() for blk in raw.split("###") if blk.strip()]

        if not blocks:
            # 이 메모에 대해 생성 실패 시 그냥 스킵 (혹은 memo 원문을 넣어도 됨)
            continue

        # 스타일 벡터와 가장 가까운 단락 선택
        best_paragraph = choose_best_sentence(blocks, style_vec)
        if not best_paragraph:
            continue

        # 이미 동일 단락이 들어가 있으면 스킵
        if best_paragraph in accumulated_diary:
            continue

        # 누적 일기에 단락 추가
        if accumulated_diary:
            accumulated_diary += "\n" + best_paragraph.strip()
        else:
            accumulated_diary = best_paragraph.strip()

    return accumulated_diary

def merge_stream(memos, style_prompt, style_examples):
    """
    스트리밍용 merge 함수 (메모 단위 생성).
    - 각 메모별로 일기 스타일 단락 하나를 생성 (stream=True)
    - 생성된 토큰을 바로바로 yield 해서 클라이언트에 라이브 버퍼링
    - style_vector / rerank는 사용하지 않음 (고퀄 모드는 merge_stream_rerank에서 처리)
    """

    # ----- 프롬프트용 텍스트 정리 -----
    if isinstance(style_prompt, dict):
        style_prompt_text = json.dumps(style_prompt, ensure_ascii=False, indent=2)
    else:
        style_prompt_text = str(style_prompt)

    if isinstance(style_examples, list):
        style_examples_text = "\n".join(f"- {s}" for s in style_examples)
    else:
        style_examples_text = "- " + str(style_examples)

    # 메모 목록 (인덱스 포함) – 참고용
    indexed_memos_text = "\n".join(f"{i+1}. {m}" for i, m in enumerate(memos))

    # system 메시지: 메모 내용 > 스타일
    system_msg = """
You are a diary-writing assistant.

Your priorities are, in this exact order:
1) FAITHFULLY reflect the concrete events from the memos.
2) Keep the chronological order of the memos.
3) Then, softly adjust tone and style to match the user's profile.

You MUST NOT invent events that are not clearly implied by the memos.
Every paragraph you write must be grounded in the given focus memo.
You are currently writing the diary ONE MEMO AT A TIME, memo by memo.
"""

    # 지금까지 생성된 일기 (다음 메모 프롬프트에 넣기 위함)
    accumulated_diary = ""

    # 이 함수 자체가 generator 역할을 하게 만들 거라서 yield 사용
    for idx, memo in enumerate(memos):
        base_sent = max(1, count_sentences(memo))
        min_sent = max(1, math.ceil(base_sent * 1.3))
        max_sent = max(min_sent, math.ceil(base_sent * 1.8))

        focus_memo_idx = idx + 1
        focus_memo = memo

        current_diary_block = (
            accumulated_diary if accumulated_diary.strip()
            else "(nothing yet)"
        )

        # 첫 메모가 아니면, 단락 간에 공백 한 줄이 있도록 유도
        start_hint = (
            "This paragraph should start on a new line, continuing naturally from the current diary.\n"
            if accumulated_diary.strip() else
            "This will be the first paragraph of the diary.\n"
        )

        prompt = f"""
You are a diary writing assistant.

You will be given:
- ONE memo: a fragmented note the user wrote today
- style profile: JSON describing the user's writing tone, phrasing preference, pacing, common expressions
- style examples: several representative sentences the user has written before
- the diary text that has already been written for previous memos

Your PRIMARY job for THIS STEP:
- Take the given memo and rewrite/expand it into a short diary-style paragraph.
- Preserve the concrete events and facts from the memo (time, place, actions, feelings).
- Do NOT invent new events that are not clearly implied by the memo.

Your SECONDARY job:
- Softly adjust the tone, rhythm, and sentence endings to match the style profile and examples.
- Style should never override the factual content of the memo.

---

STYLE PROFILE (JSON):
{style_prompt_text}

STYLE EXAMPLES (for tone only, NOT for events):
{style_examples_text}

---

ALL MEMOS (IN ORDER, WITH INDEX):
{indexed_memos_text}

CURRENT DIARY SO FAR (previous memos already processed):
{current_diary_block}

FOCUS MEMO FOR THIS STEP (memo #{focus_memo_idx}):
\"\"\"{focus_memo}\"\"\"

---

LENGTH RULE FOR THIS MEMO:
- The rewritten paragraph for this memo should be between {min_sent} and {max_sent} sentences.
- It should feel concise and natural, not repetitive.
- It should primarily focus on the events and feelings from the FOCUS MEMO.

{start_hint}
Now write ONE diary-style paragraph for ONLY this focus memo.
Output only the sentences of the paragraph, with no explanations and no numbering.
"""

        stream = client.chat.completions.create(
            model=os.getenv("GPT_MODEL", "gpt-4.1-nano"),
            stream=True,
            messages=[
                {"role": "system", "content": system_msg},
                {"role": "user",  "content": prompt},
            ],
            temperature=0.8,
            top_p=0.9,
            max_tokens=512,
        )

        # 이 메모에서 생성된 텍스트를 모아서 accumulated_diary 업데이트
        memo_generated_text = ""

        for chunk in stream:
            delta = chunk.choices[0].delta
            text = delta.content or ""
            memo_generated_text += text
            accumulated_diary += text
            # 라우트에서 그대로 쓸 수 있도록 chunk를 밖으로 전달
            yield chunk