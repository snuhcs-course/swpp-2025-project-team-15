const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const mergeController = {
    // Controller method for a single merge request (Example: POST /api/ai/merge)
    /* POST http://localhost:3000/api/ai/merge
    payload.json 파일 참고
    $ curl --no-buffer -X POST http://localhost:3000/api/ai/merge -H "Content-Type: application/json" -d @payload.json
    이걸로 버퍼링 테스트해볼 수 있음!(windows cmd 기준, sumdays-backend 위치에서)
     */
    merge: async (req, res) => {
        try {
            const { memos, style_prompt, style_examples, style_vector, end_flag } = req.body;

            if (!memos || !Array.isArray(memos) || memos.length < 2) {
                return res.status(400).json({
                    success: false,
                    message: "At least two memos are required for merging.",
                });
            }

            if (!style_prompt || !style_examples) {
                return res.status(400).json({
                    success: false,
                    message: "style_prompt and style_examples are required.",
                });
            }

            memos.sort((a, b) => a.order - b.order);
                      
            if (!end_flag) {
                const response = await axios.post(
                    `${PYTHON_SERVER_URL}/merge/`, 
                    { memos, style_prompt, style_examples, style_vector, end_flag },
                    {responseType: "stream"}
                );
                
                res.setHeader("Content-Type", "text/plain; charset=utf-8");
                response.data.pipe(res);
                return;
            } else {
                const response = await axios.post(
                    `${PYTHON_SERVER_URL}/merge/`, 
                    { memos, style_prompt, style_examples, style_vector, end_flag }
                );

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
                error: err.message || err,
            });
        }
    },
};

module.exports = mergeController;