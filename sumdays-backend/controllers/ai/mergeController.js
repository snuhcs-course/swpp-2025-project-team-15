const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const mergeController = {
    // Controller method for a single merge request (Example: POST /api/ai/merge)
    /* POST http://localhost:3000/api/ai/merge
    POSTMAN raw json
    {
    "memos": [
        {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
        {"id": 3, "content": "저녁을 가족과 먹었다.", "order": 2},
        {"id": 2, "content": "점심은 친구와 맛있게 먹었다.", "order": 3}
    ],
    "end_flag": true
    }
     */
    merge: async (req, res) => {
        try {
            const { memos, end_flag } = req.body;

            if (!memos || !Array.isArray(memos) || memos.length < 2) {
                return res.status(400).json({
                    success: false,
                    message: "At least two memos are required for merging.",
                });
            }

            memos.sort((a, b) => a.order - b.order);

            const response = await axios.post(`${PYTHON_SERVER_URL}/merge/`, { memos, end_flag });
            
            if (!end_flag) {
                if (!response.data || !response.data.merged_content) {
                    return res.status(500).json({
                        success: false,
                        message: "Invalid response from AI server.",
                        raw: response.data,
                    });
                }

                return res.status(200).json({
                    success: true,
                    merged_content: response.data.merged_content,
                });
            } else {
                // TODO: db 저장

                if (!response.data) {
                    return res.status(500).json({
                        success: false,
                        message: "Invalid response from AI server.",
                        raw: response.data,
                    });
                }
                
                return res.status(200).json({
                    success: true,
                    result: response.data,
                });
            }
            
        } catch (err) {
            console.error("[mergeController.merge] Error:", err.message);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },
};

module.exports = mergeController;