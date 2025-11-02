const request = require("supertest");
const app = require("../../app");

describe("E2E Merge API (Node → Flask → OpenAI)", () => {

  test("flag=false → merged_content 구조 검증", async () => {
    const res = await request(app)
      .post("/api/ai/merge")
      .set("Content-Type", "application/json")
      .send({
        memos: [
          { content: "아침을 굶었다.", order: 1 },
          { content: "점심도 굶었다.", order: 2 },
          { content: "저녁도 굶을거다.", order: 3 }
        ],
        end_flag: false
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(typeof res.body.merged_content).toBe("object");
    expect(typeof res.body.merged_content.merged_content).toBe("string");
    expect(res.body.merged_content.merged_content.length).toBeGreaterThan(0);
  });

  test("flag=true → 완료된 diary 결과 검증", async () => {
    const res = await request(app)
      .post("/api/ai/merge")
      .set("Content-Type", "application/json")
      .send({
        memos: [
          { content: "아침", order: 1 },
          { content: "점심", order: 2 },
          { content: "저녁", order: 3 }
        ],
        end_flag: true
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.result).toHaveProperty("diary");
    expect(res.body.result).toHaveProperty("analysis");
    expect(res.body.result).toHaveProperty("ai_comment");
  });

});

