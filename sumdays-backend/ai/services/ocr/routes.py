from flask import Blueprint, request, jsonify

# analyzer = DiaryAnalyzer()
ocr_bp = Blueprint("ocr", __name__, url_prefix="/ocr")

@ocr_bp.route("/memo", methods=["POST"])
def ocr_memo():
    """TODO: ocr-memo 구현"""

@ocr_bp.route("/diary", methods=["POST"])
def ocr_diary():
    """TODO: ocr-diary 구현"""