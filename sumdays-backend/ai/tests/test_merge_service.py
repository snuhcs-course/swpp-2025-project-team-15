import json
import numpy as np

from ai.services.merge.merge_service import (
    l2norm,
    embed_sentences,
    choose_best_sentence,
    count_sentences,
    merge_rerank,
)

# =========================
# 공용 payload 빌더
# =========================

def _build_base_payload(end_flag: bool) -> dict:
    """
    /merge/ 라우트에서 요구하는 공통 필드:
    - memos
    - end_flag
    - style_prompt
    - style_examples
    - style_vector
    """
    return {
        "memos": [
            {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
            {"id": 3, "content": "점심은 친구와 맛있게 먹었다.", "order": 2},
            {"id": 2, "content": "저녁을 가족과 먹었다.", "order": 3},
        ],
        "end_flag": end_flag,
        "style_prompt": {
            "common_phrases": ["그냥 그랬다", "피곤했다"],
            "emotional_tone": "일상적이고 약간 피곤한 느낌",
        },
        "style_examples": [
            "오늘도 그냥 그런 하루였다.",
            "피곤하지만 그래도 할 일은 했다.",
        ],
        # merge_service.merge_rerank/merge_stream에서 style_vector를 사용
        "style_vector": [1.0, 0.0, 0.0],
    }


# =========================
# 1) /merge/ 라우트 테스트
# =========================

def test_merge_only_streaming_route(client):
    """
    Case 1: end_flag=False
    - 단순 병합(스트리밍)만 수행
    - 반환: text/plain 스트리밍 응답 (전체를 모으면 하나의 일기 텍스트)
    """
    payload = _build_base_payload(end_flag=False)

    response = client.post(
        "/merge/",
        data=json.dumps(payload),
        content_type="application/json",
    )

    # 라우트에서 예외 없으면 200 + text/plain 스트리밍 응답
    assert response.status_code == 200, response.get_data(as_text=True)
    assert response.mimetype.startswith("text/plain")

    body = response.get_data(as_text=True)
    assert isinstance(body, str)
    assert body.strip() != ""  # 최소한 뭔가 텍스트가 나왔는지 확인


def test_merge_and_analyze_route(client):
    """
    Case 2: end_flag=True
    - 병합 후 일기 분석 수행
    - 반환:
      {
        "entry_date": ...,
        "user_id": ...,
        "diary": ...,
        "icon": ...,
        "ai_comment": ...,
        "analysis": {
          "keywords": ...,
          "emotion_score": ...
        }
      }
    """
    payload = _build_base_payload(end_flag=True)
    payload["entry_date"] = "2025-10-29"
    payload["user_id"] = 123

    response = client.post(
        "/merge/",
        data=json.dumps(payload),
        content_type="application/json",
    )

    assert response.status_code == 200, response.get_data(as_text=True)

    data = response.get_json()

    # 기본 필드 존재 확인
    for field in ["entry_date", "user_id", "diary", "icon", "ai_comment", "analysis"]:
        assert field in data

    assert data["entry_date"] == payload["entry_date"]
    assert data["user_id"] == payload["user_id"]
    assert isinstance(data["diary"], str)
    assert data["diary"].strip() != ""

    analysis = data["analysis"]
    assert isinstance(analysis, dict)
    assert "keywords" in analysis
    assert "emotion_score" in analysis


def test_merge_bad_request_route(client):
    """
    [에러 케이스] 필수 필드 누락 시 400 + {"error": ...}
    -> routes.py의 except 분기 커버
    """
    bad_payload = {
        # "memos" 누락
        "end_flag": False,
        "style_prompt": {},
        "style_examples": [],
        "style_vector": [],
    }

    res = client.post(
        "/merge/",
        data=json.dumps(bad_payload),
        content_type="application/json",
    )

    assert res.status_code == 400
    data = res.get_json()
    assert "error" in data


# =========================
# 2) helper 함수 테스트
# =========================

def test_l2norm_basic_and_zero():
    vec = np.array([[3.0, 4.0]], dtype=np.float32)
    out = l2norm(vec)
    # 3-4-5 삼각형 -> (0.6, 0.8)
    assert np.allclose(out, np.array([[0.6, 0.8]], dtype=np.float32), atol=1e-5)

    # zero vector도 에러 없이 0으로 유지되는지 확인
    zero = np.zeros((1, 3), dtype=np.float32)
    out_zero = l2norm(zero)
    assert np.all(out_zero == 0.0)


def test_count_sentences_various():
    assert count_sentences("") == 0
    assert count_sentences("한 문장입니다.") == 1
    assert count_sentences("첫 문장입니다. 두 번째 문장입니다!") == 2
    # 줄바꿈, ?, ! 섞여 있는 경우
    text = "하루 종일 피곤했다.\n그래도 공부는 했다?\n그래, 잘했다!"
    assert count_sentences(text) == 3


def test_choose_best_sentence_empty_candidates():
    # candidates가 비어 있으면 None 반환
    assert choose_best_sentence([], np.array([1.0, 0.0])) is None


def test_choose_best_sentence_with_mocked_embeddings(monkeypatch):
    """
    embed_sentences를 가짜로 바꿔서,
    choose_best_sentence 로직 자체만 커버
    """
    from ai.services.merge import merge_service as ms

    def fake_embed(sentences):
        # 각 문장마다 2차원 벡터 반환
        # 첫 번째 문장을 가장 유사하게 만들자.
        # style_vec = [1, 0] 가정
        return np.array(
            [
                [1.0, 0.0],   # dot=1.0
                [0.5, 0.0],   # dot=0.5
                [0.2, 0.0],   # dot=0.2
            ][: len(sentences)],
            dtype=np.float32,
        )

    monkeypatch.setattr(ms, "embed_sentences", fake_embed)

    candidates = ["첫 번째", "두 번째", "세 번째"]
    style_vec = np.array([1.0, 0.0], dtype=np.float32)
    best = choose_best_sentence(candidates, style_vec)

    assert best == "첫 번째"


def test_embed_sentences_shape_and_norm():
    """
    실제 SentenceTransformer를 한 번은 태워서
    embed_sentences 함수 자체도 커버.
    (모델이 설치되어 있어야 통과)
    """
    sentences = ["테스트 문장입니다.", "두 번째 문장입니다."]
    E = embed_sentences(sentences)

    assert isinstance(E, np.ndarray)
    assert E.shape[0] == len(sentences)
    # L2 정규화 결과의 노름이 1에 가깝다
    norms = np.linalg.norm(E, axis=-1)
    assert np.allclose(norms, 1.0, atol=1e-3)


# =========================
# 3) merge_rerank 테스트
# =========================

def test_merge_rerank_full_flow(monkeypatch):
    """
    merge_rerank 전체 로직 커버:
    - count_sentences 사용
    - OpenAI chat.completions.create mock
    - candidate 블록 없는 경우 (blocks == []) 분기
    - 이미 추가된 단락은 스킵하는 분기
    - 누적 일기 문자열 구성 (처음/이후 단락 모두)
    """
    from ai.services.merge import merge_service as ms

    # 1) embed_sentences mock → choose_best_sentence가 항상 첫 후보 선택
    def fake_embed(sentences):
        # 모든 후보 동일 방향 벡터
        return np.array([[1.0, 0.0] for _ in sentences], dtype=np.float32)

    monkeypatch.setattr(ms, "embed_sentences", fake_embed)

    # 2) OpenAI chat.completions.create mock (호출 순서에 따라 다른 결과 반환)
    class FakeMessage:
        def __init__(self, content):
            self.content = content

    class FakeChoice:
        def __init__(self, content):
            self.message = FakeMessage(content)

    class FakeResponse:
        def __init__(self, content):
            self.choices = [FakeChoice(content)]

    call_state = {"count": 0}

    def fake_create(*args, **kwargs):
        call_state["count"] += 1
        c = call_state["count"]
        if c == 1:
            # 첫 번째 메모: 후보 2개 → 첫 번째가 선택
            return FakeResponse("첫번째 단락입니다.\n###\n두번째 단락입니다.")
        elif c == 2:
            # 두 번째 메모: 후보 없음 → blocks == [] 분기
            return FakeResponse("")
        else:
            # 세 번째 메모: 첫 번째 메모와 같은 문장 → "이미 동일 단락이 들어가 있으면 스킵" 분기
            return FakeResponse("첫번째 단락입니다.\n###\n다른 단락입니다.")

    # 실제 client.chat.completions.create 덮어쓰기
    monkeypatch.setattr(ms.client.chat.completions, "create", fake_create)

    memos = [
        "첫 번째 메모 내용입니다.",
        "두 번째 메모 내용입니다.",
        "세 번째 메모 내용입니다.",
    ]
    style_prompt = {"dummy": "value"}
    style_examples = ["예시 문장"]
    style_vector = [1.0, 0.0]  # l2norm 후 [1, 0]으로 사용

    diary = merge_rerank(
        memos=memos,
        style_prompt=style_prompt,
        style_examples=style_examples,
        style_vector=style_vector,
        num_candidates=2,
    )

    # 첫 번째 호출에서 나온 첫번째 단락만 들어가 있어야 함
    assert "첫번째 단락입니다." in diary
    # 두 번째/세 번째 메모에서 추가로 같은 단락이 들어가진 않는다
    assert diary.count("첫번째 단락입니다.") == 1
    # \b 구분자를 사용해 단락 시작을 표시하는지 (구현에 맞춰 느슨하게 확인)
    assert "\b" in diary
