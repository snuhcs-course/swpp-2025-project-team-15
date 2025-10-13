const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const analyzeController = {
    // Controller method to summarize a week (Example: POST /api/ai/summarize/week)
    summarizeWeek: (req, res) => {
        // In the real application, this would call the Python AI server
        res.status(200).json({ 
            success: true, 
            message: 'summarize-week endpoint working' 
        });
    },

    // Controller method to summarize a month (Example: POST /api/ai/summarize/month)
    summarizeMonth: (req, res) => {
        res.status(200).json({ 
            success: true, 
            message: 'summarize-month endpoint working' 
        });
    },

    // Controller method for general analysis (Example: POST /api/ai/analyze)
    analyze: async (req, res) => {
        try {
            const { diary } = req.body;
            if (!PYTHON_SERVER_URL) {
                throw new Error("PYTHON_AI_URL is not set in .env file");
            }

            const response = await axios.post(`${PYTHON_SERVER_URL}/analysis/diary`, {
                diary,
            });

            return res.status(200).json({
                success: true,
                from: "python",
                result: response.data,
            });
        } catch (error) {
            console.error("Error calling Flask AI server:", error.message);
            return res.status(500).json({
                success: false,
                message: "Failed to analyze diary via AI server.",
                error: error.message,
            });
        }
    },
};

module.exports = analyzeController;