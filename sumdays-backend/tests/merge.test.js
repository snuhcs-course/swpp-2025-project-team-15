const request = require("supertest");
const axios = require("axios");
const app = require("../app");

jest.mock("axios");

describe("Merge API Tests (/api/ai/merge)", () => {

  const endpoint = "/api/ai/merge";

  //
  // 1) memos 없는 경우 → 400
  //
  test("❗ memos.length < 2 → 400", async () => {
    const res = await request(app)
      .post(endpoint)
      .send({ memos: [{ id: 1, content: "하나만", order: 1 }], end_flag: false });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });

  //
  // 2) end_flag = false 정상 케이스
  //
  test("✅ end_flag=false + 정상 응답 → 200 + merged_content", async () => {
    axios.post.mockResolvedValueOnce({
      data: { merged_content: "아침부터 저녁까지 잘 먹었다." }
    });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침 먹음", order: 1 },
          { id: 2, content: "저녁 먹음", order: 2 }
        ],
        end_flag: false
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.merged_content).toBe("아침부터 저녁까지 잘 먹었다.");
  });

  //
  // 3) end_flag = false + Flask response missing merged_content → 500
  //
  test("❗ end_flag=false + invalid AI response → 500", async () => {
    axios.post.mockResolvedValueOnce({ data: {} });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "저녁", order: 2 }
        ],
        end_flag: false
      });

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

  //
  // 4) end_flag = true 정상 케이스
  //
  test("✅ end_flag=true + 정상 응답 → 200 + result", async () => {
    axios.post.mockResolvedValueOnce({
      data: { final_text: "하루 일기 완성" }
    });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "점심", order: 2 },
          { id: 3, content: "저녁", order: 3 }
        ],
        end_flag: true
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.result).toHaveProperty("final_text");
  });

  //
  // 5) end_flag = true + AI null response → 500
  //
  test("❗ end_flag=true + invalid response → 500", async () => {
    axios.post.mockResolvedValueOnce({ data: null });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "점심", order: 2 }
        ],
        end_flag: true
      });

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

  //
  // 6) Flask 서버 자체 실패 → 500
  //
  test("❗ Flask 호출 실패 → 500", async () => {
    axios.post.mockRejectedValueOnce(new Error("Flask fail"));

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "하루 내용", order: 1 },
          { id: 2, content: "추가 내용", order: 2 }
        ],
        end_flag: false
      });

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

});

