const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const mergeController = {
    merge: async (req, res) => {
        try {
            const { memos, end_flag, style_hidden, style_examples, style_prompt } = req.body;

            if (!memos || !Array.isArray(memos) || memos.length < 2) {
                return res.status(400).json({
                    success: false,
                    message: "At least two memos are required."
                });
            }

            memos.sort((a, b) => a.order - b.order);

            // ✅ end_flag = false → Streaming Mode
            if (!end_flag) {
                const response = await fetch(`${PYTHON_SERVER_URL}/merge/stream`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ memos, style_hidden, style_examples, style_prompt, end_flag })
                });

                if (!response.ok) {
                    return res.status(500).json({ success: false, message: "AI Stream Error" });
                }

                res.setHeader("Content-Type", "text/event-stream");
                res.setHeader("Cache-Control", "no-cache");

                response.body.on("data", (chunk) => {
                    res.write(chunk.toString());
                });

                response.body.on("end", () => res.end());
                return;
            }

            // ✅ end_flag = true → Non-Streaming (완성 + 분석)
            const final = await axios.post(`${PYTHON_SERVER_URL}/merge/stream`, {
                memos,
                style_hidden,
                style_examples,
                style_prompt,
                end_flag
            });

            return res.status(200).json({
                success: true,
                result: final.data
            });

        } catch (err) {
            console.error("[mergeController.merge] Error:", err);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },
};

module.exports = mergeController;
