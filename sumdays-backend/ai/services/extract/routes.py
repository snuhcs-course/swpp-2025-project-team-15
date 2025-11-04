from flask import Blueprint, request, jsonify
from .extract_service import extract_style

extract_bp = Blueprint("extract", __name__, url_prefix="/extract")
MIN_DIARY_NUM = 3

@extract_bp.route("/style", methods=["POST"])
def extract_style_route():
    data = request.get_json()
    diaries = data.get("diaries", [])

    if len(diaries) < MIN_DIARY_NUM:
        return jsonify({"error": "At least 5 diaries required."}), 400

    return jsonify(extract_style(diaries)), 200