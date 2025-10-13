from flask import Blueprint, request, jsonify

# analyzer = DiaryAnalyzer()
stt_bp = Blueprint("stt", __name__, url_prefix="/stt")

@stt_bp.route("/memo", methods=["POST"])
def stt_memo():
    """TODO: stt-memo 구현"""
