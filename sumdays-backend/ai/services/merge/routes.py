from flask import Blueprint, request, jsonify

# analyzer = DiaryAnalyzer()
merge_bp = Blueprint("merge", __name__, url_prefix="/merge")

@merge_bp.route("/", methods=["POST"])
def merge_memo():
    """TODO: merge 구현"""

@merge_bp.route("/batch", methods=["POST"])
def merge_batch():
    """TODO: merge-batch 구현"""