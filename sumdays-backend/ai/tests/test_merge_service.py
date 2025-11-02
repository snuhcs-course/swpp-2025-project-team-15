import json

def test_merge_only(client):
    """
    Case 1: end_flag=False
    - 단순 병합만 수행
    - 반환: {"merged_content": {...}}
    """
    payload = {
        "memos": [
            {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
            {"id": 3, "content": "점심은 친구와 맛있게 먹었다.", "order": 2},
            {"id": 2, "content": "저녁을 가족과 먹었다.", "order": 3}
        ],
        "end_flag": False
    }

    response = client.post("/merge/",
                           data=json.dumps(payload),
                           content_type="application/json")
    assert response.status_code == 200, response.get_data(as_text=True)
    data = response.get_json()
    assert "merged_content" in data
    # merged_content는 dict 또는 str 형태일 수 있음
    assert isinstance(data["merged_content"], (dict, str))


def test_merge_and_analyze(client):
    """
    Case 2: end_flag=True
    - 병합 후 일기 분석 수행
    - 반환: {"entry_date": ..., "user_id": ..., "diary": ..., "icon": ..., "ai_comment": ..., "analysis": {...}}
    """
    payload = {
        "memos": [
            {"id": 1, "content": "오늘은 친구와 카페에 갔다.", "order": 1},
            {"id": 2, "content": "재밌는 대화를 나누었다.", "order": 2},
        ],
        "end_flag": True,
        "entry_date": "2025-10-29",
        "user_id": 123
    }

    response = client.post("/merge/",
                           data=json.dumps(payload),
                           content_type="application/json")
    assert response.status_code == 200, response.get_data(as_text=True)

    data = response.get_json()

    # 기본 필드 존재 확인
    for field in ["entry_date", "user_id", "diary", "icon", "ai_comment", "analysis"]:
        assert field in data

    # 분석 결과 구조 확인
    analysis = data["analysis"]
    assert isinstance(analysis, dict)
    assert "keywords" in analysis
    assert "emotion_score" in analysis
