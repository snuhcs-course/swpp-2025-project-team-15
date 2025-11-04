def build_style_prompt(style_profile):
    tone = style_profile.get("tone", "")
    formality = style_profile.get("formality", "")
    sentence_length = style_profile.get("sentence_length", "")
    sentence_structure = style_profile.get("sentence_structure", "")
    endings = ", ".join(style_profile.get("sentence_endings", []))
    lexical = style_profile.get("lexical_choice", "")
    phrases = ", ".join(style_profile.get("common_phrases", []))
    emotional = style_profile.get("emotional_tone", "")
    irony = style_profile.get("irony_or_sarcasm", "")
    slang = style_profile.get("slang_or_dialect", "")
    pacing = style_profile.get("pacing", "")
    summary = style_profile.get("overall_style_summary", "")

    return f"""
당신은 특정 사용자의 문체를 그대로 재현하는 AI입니다.

[문체 특징]
- 말투/어조: {tone}
- 격식: {formality}
- 문장 길이: {sentence_length}
- 문장 구조: {sentence_structure}
- 끝맺음: {endings}
- 단어 선택: {lexical}
- 자주 쓰는 표현: {phrases}
- 감정 온도: {emotional}
- 반어/해학: {irony}
- 속어/방언: {slang}
- 호흡 속도: {pacing}

[요약]
{summary}

스타일을 유지하는 것이 최우선입니다.
"""
