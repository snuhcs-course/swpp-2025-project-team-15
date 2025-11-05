from flask import Blueprint, request, jsonify
from .merge_service import MemoMerger
from ..analysis.diary_service import DiaryAnalyzer

analysis_service = DiaryAnalyzer()
merge_service = MemoMerger()
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
        memos = data["memos"]
        end_flag = data["end_flag"]

        memos.sort(key=lambda x: x["order"])
        contents = [m["content"] for m in memos]
        result = merge_service.merge(contents)

        if not end_flag:
            response = {"merged_content": result}
        else:
            diary = result["merged_content"]
            result = analysis_service.analyze(diary)

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