// --- 1. 모든 모킹(Mocking) 선언 ---
// jest.mock(...)은 항상 require/import 보다 위에 있어야 합니다.
jest.mock("../db/db");
jest.mock("fs");
jest.mock("axios"); // AI 컨트롤러용
jest.mock("bcrypt"); // Auth 컨트롤러용
jest.mock("jsonwebtoken"); // Auth 컨트롤러용
jest.mock('../middlewares/authMiddleware', () => (req, res, next) => {
  req.user = { userId: 123, email: 'test@test.dev' }; // 테스트용 가짜 사용자
  next();
});

// --- 2. 모듈 불러오기 ---
const request = require("supertest");
const app = require("../app");
const db = require("../db/db"); // 이미 모킹된 db를 불러옵니다.
const fs = require("fs"); // 이미 모킹된 fs를 불러옵니다.

// --- 3. 테스트 스위트 ---
describe('Daily API - High Coverage Test (Fresh Start)', () => {

  const MOCK_USER_ID = 123; // 미들웨어에서 주입한 ID와 동일하게 설정
  const MOCK_DATE = '2025-01-01';
  const MOCK_ENTRY_ID = 1;

  // 각 테스트('it')가 실행되기 전에 모든 모킹 함수의 호출 기록을 초기화합니다.
  beforeEach(() => {
    db.query.mockClear();
    fs.existsSync.mockClear();
    fs.readFileSync.mockClear();
  });

  // 모든 테스트가 끝난 후 모킹을 완전히 초기화합니다.
  afterEach(() => {
    jest.clearAllMocks();
  });

  // --- 4. getDailyEntry 테스트 ---
  describe('GET /api/db/daily/:date', () => {

    test('✅ 200: 성공 (모든 데이터 + 파일 찾음)', async () => {
      // given: 3번의 DB 조회가 모두 성공한다고 가정
      const mockEntry = { id: MOCK_ENTRY_ID, user_id: MOCK_USER_ID, entry_date: MOCK_DATE, diary: 'test', icon_name: 'icon' };
      const mockMemos = [{ content: 'memo1' }];
      const mockPhotos = [{ inner_id: 'p1', extension: 'jpg', photo_order: 1, user_id: MOCK_USER_ID, entry_date: MOCK_DATE }];

      db.query
        .mockResolvedValueOnce([ [mockEntry] ])  // 1. daily_entries 조회
        .mockResolvedValueOnce([ mockMemos ])   // 2. memos 조회
        .mockResolvedValueOnce([ mockPhotos ]); // 3. photos 조회

      fs.existsSync.mockReturnValue(true); // fs.existsSync가 true 반환
      fs.readFileSync.mockReturnValue(Buffer.from('fake_image_data')); // fs.readFileSync가 버퍼 반환

      // when:
      const res = await request(app).get(`/api/db/daily/${MOCK_DATE}`);

      // then:
      expect(res.status).toBe(200);
      expect(res.body.diary).toBe('test');
      expect(res.body.memos).toHaveLength(1);
      expect(res.body.photos[0].base64).toContain('data:image/jpg;base64');
    });

    test('✅ 200: 성공 (파일 없음 + include/exclude 쿼리)', async () => {
      // given:
      const mockEntry = { id: MOCK_ENTRY_ID, user_id: MOCK_USER_ID, entry_date: MOCK_DATE, diary: 'test', icon_name: 'icon' };
      
      db.query
        .mockResolvedValueOnce([ [mockEntry] ]) // 1. daily_entries 조회
        .mockResolvedValueOnce([ [] ])          // 2. memos 조회 (빈 배열)
        .mockResolvedValueOnce([ [] ]);          // 3. photos 조회 (빈 배열)

      fs.existsSync.mockReturnValue(false); // fs.existsSync가 false 반환 (커버리지용)

      // when: include=diary, exclude=icon_name
      const res = await request(app).get(`/api/db/daily/${MOCK_DATE}?include=diary,memos&exclude=icon_name`);
      
      // then:
      expect(res.status).toBe(200);
      expect(res.body.diary).toBe('test'); // include 확인
      expect(res.body.memos).toEqual([]);   // include 확인
      expect(res.body.icon_name).toBeUndefined(); // exclude 확인
      expect(res.body.photos).toBeUndefined();    // include 안됨
    });

    test('❗ 404: 해당 날짜 일기 없음', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [] ]); // 1. daily_entries 조회가 빈 배열 반환

      // when:
      const res = await request(app).get(`/api/db/daily/${MOCK_DATE}`);
      
      // then:
      expect(res.status).toBe(404);
      expect(res.body.message).toBe('Daily entry not found');
    });

    test('❗ 500: DB 조회 실패', async () => {
      // given:
      db.query.mockRejectedValue(new Error('DB FAILED')); // 1. DB 조회 시 에러 발생

      // when:
      const res = await request(app).get(`/api/db/daily/${MOCK_DATE}`);
      
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database query failed');
    });
  });

  // --- 5. createDailyEntry 테스트 ---
  describe('POST /api/db/daily', () => {
    
    test('✅ 201: 새 일기 생성 성공', async () => {
      // given:
      db.query
        .mockResolvedValueOnce([ [] ]) // 1. 중복 확인 (결과 없음)
        .mockResolvedValueOnce(null);  // 2. INSERT 성공

      // when:
      const res = await request(app).post('/api/db/daily').send({ date: MOCK_DATE });

      // then:
      expect(res.status).toBe(201);
      expect(res.body.message).toBe('Daily entry created successfully');
    });

    test('❗ 409: 이미 해당 날짜에 일기 존재', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [{ id: 1 }] ]); // 1. 중복 확인 (결과 있음)

      // when:
      const res = await request(app).post('/api/db/daily').send({ date: MOCK_DATE });

      // then:
      expect(res.status).toBe(409);
      expect(res.body.message).toBe('Entry already exists for this date');
    });

    test('❗ 500: DB 에러', async () => {
      // given:
      db.query.mockRejectedValue(new Error('DB FAILED')); // 1. 중복 확인 중 DB 에러

      // when:
      const res = await request(app).post('/api/db/daily').send({ date: MOCK_DATE });
      
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database insert failed');
    });
  });

  // --- 6. updateDailyEntry 테스트 ---
  describe('PATCH /api/db/daily/:date', () => {

    test('✅ 200: 일기 수정 성공 (모든 필드)', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [{ id: MOCK_ENTRY_ID }] ]); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null);                      // 2. UPDATE 성공
      
      const updateData = { icon_name: 'new_icon', diary: 'new_diary', ai_comment: 'new_comment' };

      // when:
      const res = await request(app).patch(`/api/db/daily/${MOCK_DATE}`).send(updateData);
      
      // then:
      expect(res.status).toBe(200);
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('UPDATE daily_entries SET icon_name = ?, diary = ?, ai_comment = ?'),
        ['new_icon', 'new_diary', 'new_comment', MOCK_USER_ID, MOCK_DATE]
      );
    });

    test('✅ 200: 일기 수정 성공 (일부 필드 - diary)', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [{ id: MOCK_ENTRY_ID }] ]); // 1. 일기 찾기 성공
      db.query.mockResolvedValueOnce(null);                      // 2. UPDATE 성공
      
      const updateData = { diary: 'new_diary' };

      // when:
      const res = await request(app).patch(`/api/db/daily/${MOCK_DATE}`).send(updateData);
      
      // then:
      expect(res.status).toBe(200);
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('UPDATE daily_entries SET diary = ?'),
        ['new_diary', MOCK_USER_ID, MOCK_DATE]
      );
    });

    test('❗ 400: 수정할 필드 없음', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [{ id: MOCK_ENTRY_ID }] ]); // 1. 일기 찾기 성공

      // when:
      const res = await request(app).patch(`/api/db/daily/${MOCK_DATE}`).send({}); // 빈 객체 전송
      
      // then:
      expect(res.status).toBe(400);
      expect(res.body.message).toBe('No valid fields provided for update');
    });

    test('❗ 404: 수정할 일기 없음', async () => {
      // given:
      db.query.mockResolvedValueOnce([ [] ]); // 1. 일기 찾기 실패 (빈 배열)

      // when:
      const res = await request(app).patch(`/api/db/daily/${MOCK_DATE}`).send({ diary: 'new' });
      
      // then:
      expect(res.status).toBe(404);
      expect(res.body.message).toBe('Daily entry not found');
    });

    test('❗ 500: DB 에러', async () => {
      // given:
      db.query.mockRejectedValue(new Error('DB FAILED')); // 1. 일기 찾기 중 DB 에러

      // when:
      const res = await request(app).patch(`/api/db/daily/${MOCK_DATE}`).send({ diary: 'new' });
      
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database update failed');
    });
  });

  // --- 7. deleteDailyEntry 테스트 ---
  describe('DELETE /api/db/daily/:date', () => {

    test('✅ 200: 일기 삭제 성공', async () => {
      // given:
      db.query.mockResolvedValue(null); // DELETE 쿼리 성공

      // when:
      const res = await request(app).delete(`/api/db/daily/${MOCK_DATE}`);
      
      // then:
      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Daily entry deleted successfully');
    });

    test('❗ 500: DB 에러', async () => {
      // given:
      db.query.mockRejectedValue(new Error('DB FAILED')); // DELETE 쿼리 실패

      // when:
      const res = await request(app).delete(`/api/db/daily/${MOCK_DATE}`);
      
      // then:
      expect(res.status).toBe(500);
      expect(res.body.error).toBe('Database delete failed');
    });
  });
});

