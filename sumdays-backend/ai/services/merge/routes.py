# services/merge/routes.py
from flask import Blueprint, request, Response, jsonify
from .merge_service import generate_diary
from ..analysis.diary_service import DiaryAnalyzer

merge_bp = Blueprint("merge", __name__, url_prefix="/merge")
analyzer = DiaryAnalyzer()


@merge_bp.route("/", methods=["POST"])
def merge_final():
    try:
        data = request.get_json()

        memos = [m["content"] for m in data.get("memos", [])]
        end_flag = data.get("end_flag", True)

        style_vector = data["style_vector"]
        style_prompt = data["style_prompt"]
        style_examples = data["style_examples"]


        diary = generate_diary(
            memos, style_vector, style_prompt, style_examples
        )

        if end_flag:
            result = analyzer.analyze(diary)

            return jsonify({
                "success": True,
                "diary": diary,
                "analysis": {
                    "keywords": result["keywords"],
                    "emotion_score": result["emotion_score"],
                },
                "ai_comment": result["feedback"],
                "icon": result["emoji"],
            }), 200
        
        else:
            return jsonify({
                "merged_content": diary
            }), 200

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500
