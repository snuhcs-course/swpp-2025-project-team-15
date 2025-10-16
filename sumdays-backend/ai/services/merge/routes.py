from flask import Blueprint, request, jsonify
from .merge_service import MemoMerger

merge_service = MemoMerger()
merge_bp = Blueprint("merge", __name__, url_prefix="/merge")

@merge_bp.route("/", methods=["POST"])
def merge_memo():
    """ POST http://localhost:5001/merge/ 
    \{
    "memos": [
        {"id": 1, "content": "아침을 먹었다.", "order": 1},
        {"id": 2, "content": "점심을 먹었다.", "order": 2},
        {"id": 3, "content": "저녁을 먹었다.", "order": 3}
    ]
    \}
    """
    try:
        data = request.get_json()
        memos = data["memos"]
        memos.sort(key=lambda x: x["order"])
        contents = [m["content"] for m in memos]
        result = merge_service.merge(contents)

        response = {"merged_content": result}
        return jsonify(response), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 400