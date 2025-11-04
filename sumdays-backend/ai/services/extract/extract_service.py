import torch
from transformers import AutoTokenizer, AutoModelForCausalLM
from .extract_style_profile import compute_style_profile_text

_MODEL = "EleutherAI/polyglot-ko-1.3b"
tokenizer = AutoTokenizer.from_pretrained(_MODEL)
model = AutoModelForCausalLM.from_pretrained(
    _MODEL,
    dtype=torch.bfloat16,
    device_map="auto"
).eval()


def compute_style_hidden(diaries):
    vecs = []
    for text in diaries:
        inputs = tokenizer(text, return_tensors="pt", truncation=True, max_length=512).to(model.device)
        inputs.pop("token_type_ids", None)
        with torch.no_grad():
            out = model(**inputs, output_hidden_states=True)
            hs = out.hidden_states[-1][0]  # [seq, hidden]
            mask = inputs.attention_mask[0].bool()
            vecs.append(hs[mask].mean(dim=0))

    S = torch.stack(vecs).mean(dim=0)
    S = S / (S.norm() + 1e-12)
    return S.cpu().tolist()


def extract_style_examples(diaries, n=4):
    """
    diaries: ["문단1\n문단2...", "문단3\n..."]
    step:
      1) newline('\n') 기준 문단 분리
      2) 온점('.') 기준 문장 분리
      3) 최소 길이 필터링 (len >= 8)
      4) Polyglot embedding → center similarity → top-n
    """
    # 1) 모든 일기를 하나로
    text = "\n".join(diaries)

    # 2) 문단 → 문장
    raw_candidates = []
    for paragraph in text.split("\n"):
        paragraph = paragraph.strip()
        if len(paragraph) < 2:
            continue
        # 문장 단위 분해
        sentences = [s.strip() for s in paragraph.split(".") if len(s.strip()) >= 8]
        raw_candidates.extend(sentences)

    # 후보가 너무 적으면 그대로 반환
    if len(raw_candidates) <= n:
        return raw_candidates

    # 3) 문장 embedding 계산
    vecs = []
    for s in raw_candidates:
        inputs = tokenizer(
            s, return_tensors="pt", truncation=True, max_length=512
        ).to(model.device)
        inputs.pop("token_type_ids", None)
        with torch.no_grad():
            out = model(**inputs, output_hidden_states=True)
            hs = out.hidden_states[-1][0]
            mask = inputs.attention_mask[0].bool()
            vecs.append(hs[mask].mean(dim=0))

    vecs = torch.stack(vecs)
    center = vecs.mean(dim=0, keepdim=True)
    sims = torch.cosine_similarity(vecs, center, dim=-1)
    idx = sims.topk(n).indices.tolist()

    return [raw_candidates[i] for i in idx]



def extract_style(diaries):
    style_hidden = compute_style_hidden(diaries)
    style_examples = extract_style_examples(diaries, n=4)

    # ✅ 여기서 너가 원래 원하던 LLM 기반 style_prompt 생성
    style_prompt_raw = compute_style_profile_text(diaries)

    # 문자열이 JSON일 수도, 아닐 수도 있으므로 정규화
    import json
    try:
        style_prompt = json.loads(style_prompt_raw)
    except:
        style_prompt = {"style_summary": style_prompt_raw}

    return {
        "style_hidden": style_hidden,
        "style_examples": style_examples,
        "style_prompt": style_prompt
    }
