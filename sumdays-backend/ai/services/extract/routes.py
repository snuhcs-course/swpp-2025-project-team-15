from flask import Blueprint, request, jsonify

# analyzer = DiaryAnalyzer()
extract_bp = Blueprint("extract", __name__, url_prefix="/extract")

@extract_bp.route("/style", methods=["POST"])
def extract_style():
    """TODO: extract-style 구현"""