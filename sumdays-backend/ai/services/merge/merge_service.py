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
def merge_rerank(memos, style_prompt, style_examples, style_vector, num_candidates: int = 1):

    # 프롬프트 작성용 정리
    if isinstance(style_prompt, dict):
        style_prompt_text = json.dumps(style_prompt, ensure_ascii=False, indent=2)
    else:
        style_prompt_text = str(style_prompt)

    if isinstance(style_examples, list):
        style_examples_text = "\n".join(f"- {s}" for s in style_examples)
    else:
        style_examples_text = "- " + str(style_examples)

    # style_vector 정규화
    style_vec = np.array(style_vector, dtype=np.float32).reshape(1, -1)
    style_vec = l2norm(style_vec)[0]  # (D,)

    indexed_memos_text = "\n".join(f"{i+1}. {m}" for i, m in enumerate(memos))
    accumulated_diary = ""

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
        base_sent = max(1, count_sentences(memo))
        min_sent = max(1, math.ceil(base_sent * 1.3))
        max_sent = max(min_sent, math.ceil(base_sent * 1.5))

        current_diary_block = (
            accumulated_diary if accumulated_diary.strip()
            else "(nothing yet)"
        )

        focus_memo_idx = idx + 1
        focus_memo = memo

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
            max_tokens=512,
        )

        raw = completion.choices[0].message.content or ""
        # "###" 단위로 후보 단락 나누기
        blocks = [blk.strip() for blk in raw.split("###") if blk.strip()]

        # 이 메모에 대해 생성 실패 시 그냥 스킵
        if not blocks:
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
            accumulated_diary += "\n\b" + best_paragraph.strip()
        else:
            accumulated_diary = "\b" + best_paragraph.strip()

    return accumulated_diary

### ---- merge function for streaming ---- ###
def merge_stream(memos, style_prompt, style_examples):

    # 프롬프트 작성용 정리
    if isinstance(style_prompt, dict):
        style_prompt_text = json.dumps(style_prompt, ensure_ascii=False, indent=2)
    else:
        style_prompt_text = str(style_prompt)

    if isinstance(style_examples, list):
        style_examples_text = "\n".join(f"- {s}" for s in style_examples)
    else:
        style_examples_text = "- " + str(style_examples)

    indexed_memos_text = "\n".join(f"{i+1}. {m}" for i, m in enumerate(memos))
    accumulated_diary = ""

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
        base_sent = max(1, count_sentences(memo))
        min_sent = max(1, math.ceil(base_sent * 1.3))
        max_sent = max(min_sent, math.ceil(base_sent * 1.5))

        focus_memo_idx = idx + 1
        focus_memo = memo

        current_diary_block = (
            accumulated_diary if accumulated_diary.strip()
            else "(nothing yet)"
        )

        # 첫 메모가 아니면, 단락 간에 공백 한 줄이 있도록 유도
        start_hint = (
            "This paragraph SHOULD start with a new line, continuing naturally from the current diary.\n"
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
            max_tokens=512,
        )

        memo_generated_text = ""

        for chunk in stream:
            delta = chunk.choices[0].delta
            text = delta.content or ""
            memo_generated_text += text
            accumulated_diary += text

            yield chunk