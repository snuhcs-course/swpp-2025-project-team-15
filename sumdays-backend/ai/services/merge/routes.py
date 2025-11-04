from flask import Blueprint, request, Response, jsonify, stream_with_context
from .merge_service import generate_diary_stream
from ..analysis.diary_service import DiaryAnalyzer
import json

analysis_service = DiaryAnalyzer()
merge_bp = Blueprint("merge", __name__, url_prefix="/merge")

@merge_bp.route("/", methods=["POST"])
def merge_memo():
    """ POST http://localhost:5001/merge/ 
    \{
        "memos": [
            {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
            {"id": 3, "content": "저녁을 가족과 먹었다.", "order": 2},
            {"id": 2, "content": "점심은 친구와 맛있게 먹었다.", "order": 3}
        ],
        "end_flag": true
    \}
    """
    try:
        data = request.get_json()

        memos = [m["content"] for m in sorted(data["memos"], key=lambda x: x["order"])]
        style_hidden = data["style_hidden"]
        style_prompt = data["style_prompt"]
        style_examples = data["style_examples"]

        if isinstance(style_prompt, str):
            try:
                style_prompt = json.load(style_prompt)
            except:
                return jsonify({"error": "style prompt must be JSON object"}), 400

        end_flag = data.get("end_flag", False)

        # ✅ case 1: 스트리밍 모드 (일기 작성 중)
        if not end_flag:
            def sse():
                for token in generate_diary_stream(memos, style_hidden, style_prompt, style_examples):
                    yield f"data: {token}\n\n"

            return Response(stream_with_context(sse()), mimetype="text/event-stream")

        # ✅ case 2: 완성 모드 (일기 분석까지)
        else:
            # 1) 일기 생성 → 전체 문자열로 수집
            diary = ""
            for token in generate_diary_stream(memos, style_hidden, style_prompt, style_examples):
                diary += token

            # 2) 분석 수행
            result = analysis_service.analyze(diary)

            # 3) 기존 merge API 반환 형식 유지 (중요!)
            response = {
                "entry_date": data.get("entry_date"),
                "user_id": data.get("user_id"),
                "diary": diary,
                "icon": result["emoji"],
                "ai_comment": result["feedback"],
                "analysis": {
                    "keywords": result["keywords"],
                    "emotion_score": result["emotion_score"]
                }
            }
        return jsonify(response), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 400