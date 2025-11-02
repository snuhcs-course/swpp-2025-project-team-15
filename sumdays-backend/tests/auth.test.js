const request = require("supertest");
const app = require("../app");
const db = require("../db/db");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken"); // jwt도 모킹해야 함

// 1. 의존성 모킹
jest.mock("../db/db");
jest.mock("bcrypt");
jest.mock("jsonwebtoken");

describe("Auth API 통합 테스트 (Mock DB)", () => {

  beforeEach(() => {
    // 모든 테스트 전에 모킹 기록 초기화
    db.query.mockClear();
    bcrypt.compare.mockClear();
    bcrypt.hash.mockClear();
    jwt.sign.mockClear();
  });

  describe("POST /api/auth/login", () => {
    
    test("✅ 로그인 성공 → 200", async () => {
      const mockUser = { id: 1, email: 'test@test.com', password_hash: 'hashed_pw' };
      db.query.mockResolvedValue([ [mockUser] ]);
      bcrypt.compare.mockResolvedValue(true);
      jwt.sign.mockReturnValue('fake_jwt_token'); // 가짜 토큰 반환

      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "test@test.com", password: "123" });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.token).toBe("fake_jwt_token");
      expect(db.query).toHaveBeenCalledTimes(1);
    });

    test("❗ 이메일 없음 → 401", async () => {
      db.query.mockResolvedValue([ [] ]); // DB에서 못 찾음

      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "no@test.com", password: "123" });
      
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
      expect(bcrypt.compare).not.toHaveBeenCalled(); // 비밀번호 비교조차 안 해야 함
    });

    test("❗ 비밀번호 틀림 → 401", async () => {
      const mockUser = { id: 1, email: 'test@test.com', password_hash: 'hashed_pw' };
      db.query.mockResolvedValue([ [mockUser] ]);
      bcrypt.compare.mockResolvedValue(false); // 비밀번호 틀림

      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "test@test.com", password: "wrong_pw" });
      
      expect(res.status).toBe(401);
      expect(res.body.success).toBe(false);
    });

    test("❗ 입력값 누락 → 400", async () => {
      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "test@test.com" }); // 비밀번호 누락

      expect(res.status).toBe(400);
      expect(res.body.message).toBe("이메일과 비밀번호를 모두 입력해주세요.");
      expect(db.query).not.toHaveBeenCalled(); // DB 쿼리조차 안 해야 함
    });

     test("❗ DB 에러 → 500", async () => {
      db.query.mockRejectedValue(new Error("DB Error")); // DB 에러 발생

      const res = await request(app)
        .post("/api/auth/login")
        .send({ email: "test@test.com", password: "123" });
      
      expect(res.status).toBe(500);
      expect(res.body.success).toBe(false);
    });
  });

  describe("POST /api/auth/signup", () => {

    test("✅ 회원가입 성공 → 201", async () => {
      db.query
        .mockResolvedValueOnce([ [] ]) // 1. 이메일 중복 없음
        .mockResolvedValueOnce(null); // 2. INSERT 성공 (결과값 필요 없음)
      bcrypt.hash.mockResolvedValue('new_hashed_pw'); // 비밀번호 해시

      const res = await request(app)
        .post("/api/auth/signup")
        .send({ nickname: "tester", email: "new@test.com", password: "123" });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(db.query).toHaveBeenCalledTimes(2); // 중복 확인 쿼리 1번, INSERT 쿼리 1번
      expect(bcrypt.hash).toHaveBeenCalledWith("123", 10);
    });

    test("❗ 이메일 중복 → 409", async () => {
      const mockUser = { id: 1, email: 'exist@test.com' };
      db.query.mockResolvedValue([ [mockUser] ]); // 1. 이메일 중복 *있음*

      const res = await request(app)
        .post("/api/auth/signup")
        .send({ nickname: "tester", email: "exist@test.com", password: "123" });
      
      expect(res.status).toBe(409);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toBe("이미 사용 중인 이메일입니다.");
      expect(bcrypt.hash).not.toHaveBeenCalled(); // 해시조차 안 해야 함
    });
  });
});