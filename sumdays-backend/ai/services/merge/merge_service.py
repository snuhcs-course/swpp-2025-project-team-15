import torch
import torch.nn.functional as F
from transformers import AutoTokenizer, AutoModelForCausalLM

_MODEL = "EleutherAI/polyglot-ko-1.3b"
tokenizer = AutoTokenizer.from_pretrained(_MODEL)
model = AutoModelForCausalLM.from_pretrained(
    _MODEL,
    dtype=torch.bfloat16,
    device_map="auto"
).eval()

def steer_hidden(h_t, S, alpha):
    h_n = F.normalize(h_t, dim=-1)
    S_n = F.normalize(S, dim=-1)
    proj = (h_n * S_n).sum(dim=-1, keepdim=True) * h_n
    ortho = F.normalize(S_n - proj, dim=-1)
    return h_t + alpha * ortho


def build_prompt(style_prompt, style_examples, memos):
    """
    스타일 설명 + 대표 문장 + 메모 → 하나의 프롬프트로 구성
    """
    prompt = (
        "당신은 글쓰기 보조 AI입니다. 아래의 스타일을 반드시 따르세요.\n\n"
        "=== 스타일 요약 ===\n"
        f"{style_prompt}\n\n"
        "=== 스타일 예문 ===\n"
    )

    for ex in style_examples:
        prompt += f"- {ex}\n"

    prompt += "\n=== 메모 목록 ===\n"
    for m in memos:
        prompt += f"- {m}\n"

    prompt += (
        "\n=== 지침 ===\n"
        "위 메모들을 자연스럽고 일관된 흐름으로 이어진 '하루 일기'로 작성하세요.\n"
        "목록형 금지.\n"
        "문장 간의 연결이 자연스러워야 합니다.\n"
        "감정은 과도하게 부풀리지 말고 담담하게 유지하세요.\n"
    )

    return prompt


def generate_diary_stream(memos, style_hidden, style_prompt, style_examples):
    """
    Streaming + Hidden Steering + Memo 길이 제한(1.5x)
    """
    S = torch.tensor(style_hidden, dtype=torch.bfloat16).to(model.device)
    S = S / (S.norm() + 1e-12)

    prompt = build_prompt(style_prompt, style_examples, memos)

    target_lengths = [int(len(m) * 1.5) for m in memos]
    memo_idx = 0
    lengths = [0] * len(memos)

    inputs = tokenizer(prompt, return_tensors="pt", truncation=True, max_length=512).to(model.device)
    inputs.pop("token_type_ids", None)
    input_ids = inputs["input_ids"]
    past = None

    for _ in range(1500):  # 안전한 최대 길이
        out = model(
            input_ids=input_ids[:, -1:] if past else input_ids,
            past_key_values=past,
            output_hidden_states=True,
            use_cache=True
        )
        past = out.past_key_values

        h = out.hidden_states[-1][:, -1, :]
        h_adj = steer_hidden(h, S, alpha=0.18)

        logits = model.embed_out(h_adj)
        probs = torch.softmax(logits, dim=-1)

        # 메모 길이 초과 시 문장 종료 유도
        if lengths[memo_idx] >= target_lengths[memo_idx]:
            eos = tokenizer.encode(".", add_special_tokens=False)[0]
            probs[0, eos] += 4.0

        next_token = torch.multinomial(probs, 1)
        input_ids = torch.cat([input_ids, next_token], dim=-1)

        t = tokenizer.decode(next_token[0])
        yield t

        lengths[memo_idx] += len(t)

        if t in [".", "다", "요", "\n"]:
            memo_idx = min(memo_idx + 1, len(memos) - 1)
