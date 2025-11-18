const axios = require("axios");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const MIN_DIARY_NUM = 3
const MIN_WEEKSUMMARY_NUM = 2

const analyzeController = {
    // Controller method to summarize a week (Example: POST /api/ai/summarize-week)
    /* POST http://localhost:3000/api/ai/summarize-week
    POSTMAN raw json
    {
    "user_id": 1234,
    "period": {
        "range_type": "week",
        "start_date": "2025-10-13",
        "end_date": "2025-10-19"
        }
    }
    */
    summarizeWeek: async (req, res) => {
        try {
            const { diaries } = req.body;

            if (!diaries || diaries.length < MIN_DIARY_NUM) {
                return res.status(404).json({
                    success: false,
                    message: "There must be at least 3 diaries to make weekly summary"
                });
            }

            // calculate average score and distribution
            const average_score = calculateAverageScore(diaries);
            const distribution = calculateEmotionDistribution_week(diaries);

            // get flask response
            const response = await axios.post(`${PYTHON_SERVER_URL}/analysis/week`, { diaries });
            
            if (!response.data) {
                return res.status(500).json({
                    success: false,
                    message: "Invalid response from AI server.",
                    raw: response.data,
                });
            }

            // final response
            response.data.emotion_analysis.emotion_score = average_score
            response.data.emotion_analysis.distribution = distribution


            return res.status(200).json({
                success: true,
                result: response.data,
            });
        } catch (err) {
            console.error("[analyzeController.summarizeWeek] Error:", err.message);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },

    // Controller method to summarize a month (Example: POST /api/ai/summarize-month)
    /* POST http://localhost:3000/api/ai/summarize-month
    POSTMAN raw json
    {
    "user_id": 1234,
    "period": {
        "range_type": "month",
        "start_date": "2025-10-01",
        "end_date": "2025-10-31"
        }
    }
    // */
    // summarizeMonth: async (req, res) => {
    //     try {
    //         const { user_id, period } = req.body;

    //         if (!period || period.range_type !== "month") {
    //             return res.status(400).json({
    //                 success: false,
    //                 message: "Not a proper input."
    //             })
    //         }
            
    //         // TODO: get data(weeks) from db(mock for now)
    //         const weeks = await getUserWeeksMock(user_id, period);
    //         if (!weeks || weeks.length < MIN_WEEKSUMMARY_NUM) {
    //             return res.status(404).json({
    //                 success: false,
    //                 message: "There must be at least 2 week summaries to make monthly summary"
    //             });
    //         }

    //         // calculate average score and distribution
    //         const average_score = calculateAverageScore(weeks);
    //         const distribution = calculateEmotionDistribution_month(weeks);

    //         // get flask response
    //         const response = await axios.post(`${PYTHON_SERVER_URL}/analysis/month`, { weeks });
            
    //         if (!response.data) {
    //             return res.status(500).json({
    //                 success: false,
    //                 message: "Invalid response from AI server.",
    //                 raw: response.data,
    //             });
    //         }

    //         // final response
    //         response.data.summary.emotion_score = average_score
    //         response.data.summary.emotion_statistics = distribution

    //         return res.status(200).json({
    //             success: true,
    //             result: response.data,
    //         });
    //     } catch (err) {
    //         console.error("[analyzeController.summarizeMonth] Error:", err.message);
    //         return res.status(500).json({
    //             success: false,
    //             error: err.message,
    //         });
    //     }
    // },

    // Controller method for daily diary analysis (Example: POST /api/ai/analyze)
    /* POST http://localhost:3000/api/ai/analyze
    POSTMAN raw json
    {"diary":"오늘은 산책하면서 생각이 많았던 하루였다."}
    */
    analyze: async (req, res) => {
        try {
            const { diary } = req.body;

            if (!diary) {
                return res.status(400).json({
                    success: false,
                    message: "No diary input."
                })
            }

            const response = await axios.post(`${PYTHON_SERVER_URL}/analysis/diary`, { diary });
            
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
        } catch (err) {
            console.error("[analyzeController.analyze] Error:", err.message);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },
};


/**
 * calculate average emotion score (for both month and week)
 */
function calculateAverageScore(data) {
    const emotion_scores = data.map(d => d.emotion_score || 0);
    const result = emotion_scores.reduce((a,b)=>a+b, 0)/emotion_scores.length;
    return Number(result.toFixed(2))
}

/**
 * calculate emotion score distribution for week 
 */
function calculateEmotionDistribution_week(diaries) {
    const dist = { positive: 0, neutral: 0, negative: 0 };
    diaries.forEach(d => {
        if (d.emotion_score > 0.2) dist.positive++;
        else if (d.emotion_score < -0.2) dist.negative++;
        else dist.neutral++;
    });
    return dist;
}

/**
 * calculate emotion score distribution for month 
 */
// function calculateEmotionDistribution_month(weeks) {
//     const total = { positive: 0, neutral: 0, negative: 0 };
//     weeks.forEach(w => {
//         const dist = w.distribution || { positive: 0, neutral: 0, negative: 0 };
//         total.positive += dist.positive || 0;
//         total.neutral += dist.neutral || 0;
//         total.negative += dist.negative || 0;
//     });
    
//     const sum = total.positive + total.neutral + total.negative;
//     if (sum > 0) {
//         total.positive = +(total.positive / sum).toFixed(2);
//         total.neutral  = +(total.neutral  / sum).toFixed(2);
//         total.negative = +(total.negative / sum).toFixed(2);
//     }

//     return total;
// }

/**
 * Temporary mock function for fetching diaries until DB is implemented
 * Later, replace this with DB call like:
 * `await diaryModel.getUserDiariesByPeriod(user_id, start_date, end_date)`
 */
async function getUserDiariesMock(user_id, period) {
    return [
        {
        "date": "2025-10-13",
        "diary": "오늘부터 본격적으로 시험 공부를 시작했다. 오랜만에 책을 오래 보니 집중이 잘 안 된다. 그래도 계획표를 세워두니 마음이 조금 안정됐다.",
        "emoji": "📚",
        "emotion_score": 0.2
        },
        {
        "date": "2025-10-14",
        "diary": "도서관에서 하루 종일 공부했다. 커피를 두 잔이나 마셨지만 졸음을 참기 힘들었다. 그래도 목표했던 양을 끝내서 뿌듯했다.",
        "emoji": "☕",
        "emotion_score": 0.5
        },
        {
        "date": "2025-10-15",
        "diary": "어제 외운 내용이 기억이 잘 나지 않아 속상했다. 나만 뒤처지는 것 같아 불안했다. 그래도 포기하지 않고 조금씩 다시 정리했다.",
        "emoji": "😣",
        "emotion_score": -0.4
        },
        {
        "date": "2025-10-16",
        "diary": "스터디 친구들과 문제를 풀었다. 서로 모르는 부분을 설명해주며 공부하니 재미있었다. 덕분에 자신감이 조금 생겼다.",
        "emoji": "🧠",
        "emotion_score": 0.6
        },
        {
        "date": "2025-10-17",
        "diary": "오늘은 집중이 잘 안 돼서 괜히 SNS만 봤다. 하루가 헛되게 지나간 것 같아 자책했다. 내일부터는 정말 정신 차려야겠다.",
        "emoji": "😔",
        "emotion_score": -0.5
        },
        {
        "date": "2025-10-18",
        "diary": "시험이 코앞이라 긴장감이 높아졌다. 그래도 지금까지 공부한 걸 복습하면서 조금 안심이 됐다. 친구들과 응원 메시지를 주고받았다.",
        "emoji": "✏️",
        "emotion_score": 0.3
        },
        {
        "date": "2025-10-19",
        "diary": "오늘은 드디어 시험이 끝났다! 손에 쥔 펜이 가볍게 느껴졌다. 결과는 모르지만 최선을 다했으니 만족스럽다. 시험이 끝난 해방감이 너무 좋다.",
        "emoji": "🎉",
        "emotion_score": 0.9
        }
    ];
}

/**
 * Temporary mock function for fetching weeks until DB is implemented
 * Later, replace this with DB call like:
 * `await diaryModel.getUserWeekssByPeriod(user_id, start_date, end_date)`
 */
async function getUserWeeksMock(user_id, period) {
    return [
        {
        "week_range": "2025-10-01~2025-10-07",
        "title": "불안한 시작, 혼란스러운 마음",
        "overview": "새로운 프로젝트와 일정이 겹치며 정신없는 한 주였다. 실수도 많았고 자신감이 떨어졌다. 해야 할 일은 많은데 집중이 되지 않아 스스로를 자책했다.",
        "average_emotion": -0.5,
        "dominant_emoji": "😔",
        "keywords": ["스트레스", "혼란", "불안", "피로", "실수"]
        },
        {
        "week_range": "2025-10-08~2025-10-14",
        "title": "균형을 찾아가는 시도",
        "overview": "하루하루 계획을 세우고 루틴을 만들며 조금씩 안정감을 되찾았다. 동료와의 대화 속에서 위로를 받았고, 짧은 산책이나 음악 감상이 도움이 되었다.",
        "average_emotion": 0.1,
        "dominant_emoji": "🙂",
        "keywords": ["루틴", "회복", "균형", "대화", "휴식"]
        },
        {
        "week_range": "2025-10-15~2025-10-21",
        "title": "자신감을 되찾다",
        "overview": "프로젝트의 중간 발표를 성공적으로 마치며 자신감을 얻었다. 주변의 격려 덕분에 성취감이 커졌다. 남은 과제들에 대한 의욕이 생겼다.",
        "average_emotion": 0.6,
        "dominant_emoji": "😄",
        "keywords": ["성취감", "자신감", "성장", "격려", "진전"]
        },
        {
        "week_range": "2025-10-22~2025-10-28",
        "title": "안정된 리듬 속의 성숙",
        "overview": "일과 휴식의 균형이 잘 잡히면서 감정이 안정되었다. 스스로의 한계를 인정하고, 완벽하지 않아도 괜찮다는 생각을 하게 되었다. 마음의 여유가 생겼다.",
        "average_emotion": 0.4,
        "dominant_emoji": "😊",
        "keywords": ["안정", "여유", "균형", "성숙", "마음의 평화"]
        },
        {
        "week_range": "2025-10-29~2025-10-31",
        "title": "새로운 다짐, 다음을 향해",
        "overview": "한 달을 돌아보며 많이 성장했음을 느꼈다. 여전히 부족한 점은 있지만, 스스로를 믿고 나아가기로 다짐했다. 조용하지만 긍정적인 마무리였다.",
        "average_emotion": 0.8,
        "dominant_emoji": "🌟",
        "keywords": ["다짐", "희망", "자기성장", "긍정", "미래"]
        }
    ];    
}

module.exports = analyzeController;