from flask import Blueprint, request, jsonify, Response
from .merge_service import merge_stream
from ..analysis.diary_service import DiaryAnalyzer

analysis_service = DiaryAnalyzer()
merge_bp = Blueprint("merge", __name__, url_prefix="/merge")

@merge_bp.route("/", methods=["POST"])
def merge_memo():
    """ POST http://localhost:5001/merge/ 
    \{
        "memos": [
            {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
            {"id": 3, "content": "저녁을 가족과 먹었다.", "order": 2},
            {"id": 2, "content": "점심은 친구와 맛있게 먹었다.", "order": 3}
        ],
        "end_flag": true,
        "style_prompt": {...},
        "style_examples": {"adsf", "asdfaf", "FKSJD"}
    \}
    """
    try:
        data = request.get_json()
        memos =  [m["content"] for m in sorted(data["memos"], key=lambda x: x["order"])]
        end_flag = data["end_flag"]
        style_prompt = data["style_prompt"]
        style_examples = data["style_examples"]
        style_vector = data["style_vector"]

        if not end_flag:
            def generate():
                for chunk in merge_stream(memos, style_prompt, style_examples):
                    # OpenAI stream chunk에서 텍스트만 뽑기
                    delta = chunk.choices[0].delta
                    content = delta.content or ""
                    if not content:
                        continue
                    # 여기서는 그냥 raw 텍스트를 넘긴다고 가정
                    # 필요하면 JSON 줄단위로 감싸도 됨 (e.g. yield json.dumps({"content": content}) + "\n")
                    yield content

            # text/plain으로 쭉 흘려보내는 방식
            return Response(generate(), mimetype="text/plain; charset=utf-8")
            # return merge_stream(memos, style_prompt, style_examples, style_vector)

            # def generate():
            #     for chunk in stream:
            #         if chunk.choices and chunk.choices[0].delta.content:
            #             yield chunk.choices[0].delta.content

            # return Response(generate(), mimetype="text/plain; charset=utf-8")
        else:
            diary = merge_stream(memos, style_prompt, style_examples, style_vector)

            # diary = ""
            # for chunk in stream:
            #     if chunk.choices and chunk.choices[0].delta.content:
            #         diary += chunk.choices[0].delta.content

            result = analysis_service.analyze(diary)

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
    # try:
    #     data = request.get_json()
    #     memos = data["memos"]
    #     end_flag = data["end_flag"]

    #     memos.sort(key=lambda x: x["order"])
    #     contents = [m["content"] for m in memos]
    #     result = merge_service.merge(contents)

    #     if not end_flag:
    #         response = {"merged_content": result}
    #     else:
    #         diary = result["merged_content"]
    #         result = analysis_service.analyze(diary)

    #         response = {
    #             "entry_date": data.get("entry_date"),
    #             "user_id": data.get("user_id"),
    #             "diary": diary,
    #             "icon": result["emoji"],
    #             "ai_comment": result["feedback"],
    #             "analysis": {
    #                 "keywords": result["keywords"],
    #                 "emotion_score": result["emotion_score"]
    #             }
    #         }
    #     return jsonify(response), 200

    # except Exception as e:
    #     return jsonify({"error": str(e)}), 400