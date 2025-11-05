from flask import Blueprint, request, jsonify
from ..analysis.diary_service import DiaryAnalyzer
from .merge_service import merge_with_style
import json

analysis_service = DiaryAnalyzer()
merge_bp = Blueprint("merge", __name__, url_prefix="/merge")
analyzer = DiaryAnalyzer()


@merge_bp.route("/", methods=["POST"])
def merge_memo():
    """ POST http://localhost:5001/merge/ 

    """
    try:
        data = request.get_json()

        memos = data["memos"]
        end_flag = data["end_flag"]
        style_vector = data["style_vector"]
        style_profile = data["style_profile"]
        style_examples = data["style_examples"]

        # style_profile(str) json 형식으로 변환
        if isinstance(style_profile, str):
            try:
                style_profile = json.loads(style_profile)
            except:
                return jsonify({"error": "style_profile must be JSON object"}), 400

        contents = [m["content"] for m in sorted(memos, key=lambda x: x["order"])]
        result = merge_with_style(contents, style_vector, style_profile, style_examples)

        if end_flag:
            return analysis_service.analyze(result)        

        return jsonify({"merged_content": result}), 200


    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500
