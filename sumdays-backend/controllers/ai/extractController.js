const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const extractController = {
    extractStyle: async (req, res) => {
        try {
            const { diaries } = req.body;

            if (!diaries || !Array.isArray(diaries) || diaries.length < 3) {
                return res.status(400).json({
                    success: false,
                    message: "At least 3 diaries are required."
                });
            }

            const response = await axios.post(`${PYTHON_SERVER_URL}/extract/style`, {
                diaries
            });

            return res.status(200).json({
                success: true,
                style_vector: response.data.style_vector,
                style_examples: response.data.style_examples,
                style_prompt: response.data.style_prompt
            });

        } catch (err) {
            console.error("[extractController.extractStyle] Error:", err.message);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },
};

module.exports = extractController;
