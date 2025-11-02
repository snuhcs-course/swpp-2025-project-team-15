const request = require("supertest");
const axios = require("axios");
const app = require("../app");

jest.mock("axios");

describe("Analyze + Weekly + Monthly Summary", () => {

  test("analyze → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        ai_comment: "친구들과의 대화로 기분이 좋아진 하루였습니다.",
        analysis: { emotion_score: 0.7, keywords: ["친구들"] },
        diary: "오늘은 친구들과 카페에 가서 이야기를 많이 나눴다.",
        entry_date: null,
        icon: "😊",
        user_id: null
      }
    });

    const res = await request(app)
      .post("/api/ai/analyze")
      .send({ diary: "오늘은 친구들과..." });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  test("summarize-week → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        emotion_analysis: { emotion_score: 0, trend: "increasing" },
        highlights: [],
        insights: {},
        summary: {}
      }
    });

    const res = await request(app)
      .post("/api/ai/summarize-week")
      .send({
        user_id: 1,
        period: { range_type: "week" }
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  test("summarize-month → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        insights: {},
        summary: { emotion_score: 0 },
        weeks: []
      }
    });

    const res = await request(app)
      .post("/api/ai/summarize-month")
      .send({
        user_id: 1,
        period: { range_type: "month" }
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

});

test("analyze → diary 없음 → 400", async () => {
  const res = await request(app)
    .post("/api/ai/analyze")
    .send({}); // diary 없음
  expect(res.status).toBe(400);
  expect(res.body.success).toBe(false);
});

test("analyze → Flask 실패 → 500", async () => {
  axios.post.mockRejectedValueOnce(new Error("flask died"));
  const res = await request(app)
    .post("/api/ai/analyze")
    .send({ diary: "text" });
  expect(res.status).toBe(500);
  expect(res.body.success).toBe(false);
});

test("summarize-week → range_type != week → 400", async () => {
  const res = await request(app)
    .post("/api/ai/summarize-week")
    .send({ user_id: 1, period: { range_type: "month" } });
  expect(res.status).toBe(400);
});

test("summarize-month → range_type != month → 400", async () => {
  const res = await request(app)
    .post("/api/ai/summarize-month")
    .send({ user_id: 1, period: { range_type: "week" } });
  expect(res.status).toBe(400);
});

test("summarize-month → Flask 실패 → 500", async () => {
  axios.post.mockRejectedValueOnce(new Error("flask died"));
  const res = await request(app)
    .post("/api/ai/summarize-month")
    .send({ user_id: 1, period: { range_type: "month" } });
  expect(res.status).toBe(500);
  expect(res.body.success).toBe(false);
});

