import tiktoken

def make_logit_bias(style_profile):
    # 강화 대상 표현들
    endings = style_profile.get("sentence_endings", []) or []
    phrases = style_profile.get("common_phrases", []) or []
    targets = endings + phrases

    # GPT-4.1-mini tokenizer 로딩
    enc = tiktoken.encoding_for_model("gpt-4.1-mini")

    bias = {}

    for phrase in targets:
        # multi-token 형태 그대로 인코딩
        token_ids = enc.encode(phrase)

        # 각 토큰에 bias 적용
        for tok in token_ids:
            bias[str(tok)] = 2.0  # 강도 조절 가능 (1.5 ~ 2.5 추천)

    return bias
