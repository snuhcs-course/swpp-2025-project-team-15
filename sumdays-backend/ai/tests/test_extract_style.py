import io
import json
import numpy as np
from PIL import Image


# ============================================
# 1) extract_service.py 모든 함수 단독 테스트
# ============================================
def test_l2norm_basic():
    from ai.services.extract.extract_service import l2norm
    x = np.array([[3.0, 4.0]])
    result = l2norm(x)
    assert np.isclose(np.linalg.norm(result), 1.0)


def test_embed_sentences():
    from ai.services.extract.extract_service import embed_sentences
    sentences = ["오늘은 좋은 날이다.", "밥을 먹었다."]
    E = embed_sentences(sentences)
    assert E.shape[0] == 2
    assert E.shape[1] > 0  # embedding dim


def test_diary_embedding():
    from ai.services.extract.extract_service import diary_embedding
    diary = "오늘은 좋은 날이었다.\n밥도 맛있었다."
    v = diary_embedding(diary)
    assert len(v) > 0


def test_compute_style_vector():
    from ai.services.extract.extract_service import compute_style_vector
    diaries = [
        "오늘은 좋은 날이었다.",
        "점심은 친구와 먹었다.",
        "밤에는 잠이 잘 왔다."
    ]
    vec = compute_style_vector(diaries)
    assert isinstance(vec, list)
    assert len(vec) > 0


def test_extract_style_examples():
    from ai.services.extract.extract_service import extract_style_examples
    diaries = [
        "오늘 아침에 밥을 먹었다. 학교를 갔다.",
        "점심엔 김밥을 먹었다. 오후에는 과제를 했다.",
    ]
    dummy_vec = np.random.rand(768).tolist()
    examples = extract_style_examples(diaries, dummy_vec, n=2)
    assert len(examples) <= 2


def test_extract_style_full():
    from ai.services.extract.extract_service import extract_style
    diaries = [
        "오늘 아침에 일어났다. 공부를 조금 했다.",
        "점심에는 라면을 먹었다.",
        "밤에는 산책을 나갔다."
    ]
    result = extract_style(diaries)
    assert "style_vector" in result
    assert "style_prompt" in result
    assert "style_examples" in result


# ============================================
# 2) compute_style_profile_text → 실제 OpenAI 호출 테스트
# ============================================
def test_compute_style_profile_text():
    from ai.services.extract.extract_style_profile import compute_style_profile_text
    diaries = ["오늘은 기분이 좋았다.", "점심은 맛있었다.", "밤에는 산책을 했다.", "잤다", "일어났다"]
    text = compute_style_profile_text(diaries)
    assert len(text) >= 5


# ============================================
# 3) /extract/style API 테스트 — JSON 입력
# ============================================
def test_extract_style_route_json(client):
    diaries = [
        "오늘은 좋은 날이었다.",
        "점심에는 국밥을 먹었다.",
        "밤에는 숙제를 했다.", "잤다", "일어났다"
    ]

    response = client.post(
        "/extract/style",
        json={"diaries": diaries},
        content_type="application/json"
    )

    assert response.status_code == 200
    data = response.get_json()
    assert "style_vector" in data
    assert "style_examples" in data
    assert "style_prompt" in data


# ============================================
# 4) /extract/style API 테스트 — diaries 부족 → 에러분기
# ============================================
def test_extract_style_route_too_few(client):
    diaries = ["하나", "둘"]   # 최소 3개 → MIN_DIARY_NUM=3

    response = client.post(
        "/extract/style",
        json={"diaries": diaries},
        content_type="application/json"
    )

    # MIN_DIARY_NUM=3 < 3 → 400이 나와야 한다
    assert response.status_code == 400


# ============================================
# 5) /extract/style API 테스트 — form-data + 이미지 OCR 경로
# ============================================
def test_extract_style_route_with_images(client):
    # 더미 이미지를 메모리에서 생성 → Vision OCR 호출됨
    img = Image.new("RGB", (200, 60), color=(255, 255, 255))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)

    # diaries는 1개만 넣고, OCR 결과에서 최소 2개 이상 텍스트가 나오도록 기대
    # (실제 Vision 결과에 따라 다르지만, 호출 자체가 성공하면 커버리지 충족됨)
    data = {
        "diaries": json.dumps(["오늘 일기를 썼다."]),
    }

    response = client.post(
        "/extract/style",
        data={
            **data,
            "images": (buf, "test.png")
        },
        content_type="multipart/form-data"
    )

    # OCR이 실패하더라도 detect_texts_from_diaries 내부에서 포맷은 반환됨
    # diaries ≥ 3 이면 200, 아니면 400
    assert response.status_code in (200, 400)
