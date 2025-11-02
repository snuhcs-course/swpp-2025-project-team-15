// --- 1. 모든 모킹(Mocking) 선언 ---
// jest.mock(...)은 항상 require/import 보다 위에 있어야 합니다.
jest.mock("../db/db");
jest.mock("fs");
jest.mock("axios");
jest.mock("bcrypt");
jest.mock("jsonwebtoken");
jest.mock('../middlewares/authMiddleware', () => (req, res, next) => {
  req.user = { userId: 1, email: 'test@user.com' };
  next();
});

// --- 2. 모듈 불러오기 ---
const request = require("supertest");
const app = require("../app");
const db = require("../db/db");

// --- 3. 테스트 스위트 ---
describe('Memos API - High Coverage Test (Fresh Start)', () => {

  const MOCK_USER_ID = 1;
  const MOCK_DATE = '2025-01-01';
  const MOCK_ENTRY_ID = 10;
  const MOCK_INNER_ID = 'memo-1';

  afterEach(() => {
    jest.clearAllMocks();
  });

  // --- 4. getDailyEntryId 헬퍼 함수에 대한 모킹 ---
  // (getDailyEntryId가 [rows]로 구조분해하므로 [[]]를 반환해야 함)
  const mockFindEntry = () => {
    db.query.mockResolvedValueOnce([ [{ id: MOCK_ENTRY_ID }] ]);
  };
  const mockFindNoEntry = () => {
    db.query.mockResolvedValueOnce([ [] ]); // 404 테스트용
  };
  const mockDbError = () => {
    db.query.mockRejectedValue(new Error('DB FAILED')); // 500 테스트용
  };

  // --- 5. createMemo 테스트 ---
  describe('POST /api/db/daily/memos/:date', () => {

    test('✅ 201: 메모 생성 성공', async () => {
      // given: 
      mockFindEntry(); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null); // 2. INSERT 성공

      const newMemo = { inner_id: MOCK_INNER_ID, memo_order: 1, content: 'new memo' };

      // when:
      const res = await request(app)
        .post(`/api/db/daily/memos/${MOCK_DATE}`)
        .send(newMemo);
        
      // then:
      expect(res.status).toBe(201);
      expect(res.body.message).toBe('memo created successfully');
    });

    test('❗ 404: Daily Entry 없음', async () => {
      // given:
      mockFindNoEntry(); // 1. 일기 찾기 실패

      // when:
      const res = await request(app)
        .post(`/api/db/daily/memos/${MOCK_DATE}`)
        .send({ inner_id: MOCK_INNER_ID, memo_order: 1, content: 'new memo' });

      // then:
      expect(res.status).toBe(404);
    });

    test('❗ 500: DB 에러', async () => {
      // given:
      mockDbError(); // 1. 일기 찾기 중 DB 에러

      // when:
      const res = await request(app)
        .post(`/api/db/daily/memos/${MOCK_DATE}`)
        .send({ inner_id: MOCK_INNER_ID, memo_order: 1, content: 'new memo' });
        
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database insert failed');
    });
  });

  // --- 6. updateMemo 테스트 ---
  describe('PATCH /api/db/daily/memos/:date/:inner_id', () => {
    
    test('✅ 200: 메모 수정 성공 (모든 필드)', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null); // 2. UPDATE 성공
      
      const updateData = { memo_order: 2, content: "updated", memo_time: "10:00:00" };
      
      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`)
        .send(updateData);
        
      // then:
      expect(res.status).toBe(200);
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('UPDATE memos SET'),
        [2, "updated", "10:00:00", MOCK_ENTRY_ID, MOCK_INNER_ID]
      );
    });

    test('✅ 200: 메모 수정 성공 (일부 필드)', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null); // 2. UPDATE 성공
      
      const updateData = { content: "updated only" };
      
      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`)
        .send(updateData);
        
      // then:
      expect(res.status).toBe(200);
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('UPDATE memos SET content = ?'),
        ["updated only", MOCK_ENTRY_ID, MOCK_INNER_ID]
      );
    });

    test('❗ 400: 수정할 필드 없음', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      
      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`)
        .send({}); // 빈 객체
        
      // then:
      expect(res.status).toBe(400);
      expect(res.body.message).toBe('No valid fields provided for update');
    });

    test('❗ 500: DB 에러 (UPDATE)', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      mockDbError(); // 2. UPDATE 중 에러

      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`)
        .send({ content: "updated" });

      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database delete failed');
    });
  });

  // --- 7. deleteMemo 테스트 ---
  describe('DELETE /api/db/daily/memos/:date/:inner_id', () => {
    
    test('✅ 200: 메모 삭제 성공', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null); // 2. DELETE 성공
      
      // when:
      const res = await request(app)
        .delete(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`);
        
      // then:
      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Memo deleted successfully');
    });

    test('❗ 500: DB 에러 (DELETE)', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      mockDbError(); // 2. DELETE 중 에러

      // when:
      const res = await request(app)
        .delete(`/api/db/daily/memos/${MOCK_DATE}/${MOCK_INNER_ID}`);

      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database delete failed');
    });
  });

  // --- 8. reorderMemos 테스트 ---
  describe('PATCH /api/db/daily/memos/:date/order', () => {
    
    test('✅ 200: 메모 순서 변경 성공', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      db.query.mockResolvedValue(null); // 2. 모든 UPDATE 쿼리 성공
      
      const orderMap = { "memo-1": 2, "memo-2": 1 };
      
      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/order`)
        .send(orderMap);
        
      // then:
      expect(res.status).toBe(200);
      // getDailyEntryId 1번 + UPDATE 2번 = 총 3번
      expect(db.query).toHaveBeenCalledTimes(3); 
    });

    test('❗ 500: DB 에러 (Promise.all 실패)', async () => {
      // given:
      mockFindEntry(); // 1. 일기 찾기 성공
      mockDbError(); // 2. UPDATE 쿼리 실패
      
      const orderMap = { "memo-1": 2, "memo-2": 1 };
      
      // when:
      const res = await request(app)
        .patch(`/api/db/daily/memos/${MOCK_DATE}/order`)
        .send(orderMap);
        
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database update failed');
    });
  });
});

