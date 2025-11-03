from flask import Blueprint, request, jsonify
from .extract_service import compute_style_profile_text, compute_style_vector

extract_bp = Blueprint("extract", __name__, url_prefix="/extract")
MIN_DIARY_NUM = 3

@extract_bp.route("/style", methods=["POST"])
def extract_style():
    data = request.get_json()
    diaries = data.get("diaries", [])

    if len(diaries) < MIN_DIARY_NUM:
        return jsonify({"error": "At least 5 diaries required."}), 400

    style_vector = compute_style_vector(diaries)
    style_profile_text = compute_style_profile_text(diaries)

    return jsonify({
        "style_vector": style_vector,
        "style_profile": style_profile_text
    }), 200