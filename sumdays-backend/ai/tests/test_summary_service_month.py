import json

def test_analyze_month(client):
    """POST /analysis/month"""
    payload = {
        "weeks": [
            {"overview": "이번 주는 행복했다.", "title": "행복한 주"},
            {"overview": "다음 주는 힘들었지만 배웠다.", "title": "도전의 주"}
        ]
    }

    response = client.post("/analysis/month",
                           data=json.dumps(payload),
                           content_type="application/json")
    assert response.status_code == 200, response.get_data(as_text=True)

    data = response.get_json()
    assert "summary" in data
    assert "insights" in data
    summary = data["summary"]

    assert "title" in summary
    assert "overview" in summary
    assert "dominant_emoji" in summary
    assert "emerging_topics" in summary
    assert "emotion_statistics" in summary
    assert "emotion_score" in summary

def test_analysis_month_invalid_body(client):
    """
    [에러 테스트] /analysis/month - weeks 누락
    """
    res = client.post("/analysis/month", data=json.dumps({"bad_key": []}),
                      content_type="application/json")
    data = res.get_json()

    assert res.status_code == 400
    assert "error" in data