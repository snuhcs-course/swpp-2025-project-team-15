from flask import Blueprint, request, jsonify
from .diary_service import DiaryAnalyzer

analyzer = DiaryAnalyzer()
analysis_bp = Blueprint("analysis", __name__, url_prefix="/analysis")

@analysis_bp.route("/diary", methods=["POST"])
def analyze_diary():
    """ POST http://localhost:5001/analysis/diary
    \{"diary": "오늘은 친구들과 카페에 가서 이야기를 많이 나눴다."\}
    """
    try:
        data = request.get_json()
        diary = data.get("diary", "")
        result = analyzer.analyze(diary)

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
    
@analysis_bp.route("/week", methods=["POST"])
def analyze_week():
    """TODO: summarize-week 구현"""

@analysis_bp.route("/month", methods=["POST"])
def analyze_month():
    """TODO: summarize-month 구현"""