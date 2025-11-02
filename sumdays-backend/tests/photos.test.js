// 테스트할 컨트롤러 로드
const photosController = require('./photosController');

// 💡 핵심: db.js 모듈을 가짜(Mock)로 대체합니다.
// 이렇게 하면 실제 db.js 파일 대신 jest가 만든 가짜 모듈을 사용하게 됩니다.
const db = require('../db/db'); 
jest.mock('../db/db'); // db 모듈 모킹

// (참고) 만약 db.js가 { pool: ... } 이런 식이라면 jest.mock('../db/db', () => ({ pool: { query: jest.fn() } })); 처럼 구체화합니다.
// 여기서는 db.js가 query 함수를 바로 export 했다고 가정합니다. 예: module.exports = { query: ... }
// 만약 db.js가 pool 객체를 export 한다면:
// jest.mock('../db/db', () => ({
//   query: jest.fn(),
// }));


describe('photosController 유닛 테스트', () => {

  // --------------------------------------------------
  //  POST /photos (사진 생성) 테스트
  // --------------------------------------------------
  describe('createPhoto', () => {
    
    let mockRequest;
    let mockResponse;

    // 각 테스트('it')가 실행되기 직전에 매번 실행됩니다.
    beforeEach(() => {
      // 1. 가짜 req 객체 준비
      //    (authMiddleware가 넣어줬다고 가정한 req.user 포함)
      mockRequest = {
        body: {
          title: '테스트 사진',
          description: '테스트 설명'
        },
        file: { // (참고) 파일 업로드(multer 등)를 쓴다면 req.file도 모킹
          path: 'uploads/fake_image.jpg' 
        },
        user: { // authMiddleware가 넣어준 사용자 정보
          id: 1 
        }
      };

      // 2. 가짜 res 객체 준비 (spy 함수들)
      mockResponse = {
        status: jest.fn(() => mockResponse), // .status()가 자신(res)을 반환해야 .json()을 체이닝 가능
        json: jest.fn(),
      };
      
      // 3. Mock 리셋: 이전에 호출된 기록을 모두 지웁니다.
      db.query.mockClear();
      mockResponse.status.mockClear();
      mockResponse.json.mockClear();
    });

    // --- 성공 케이스 ---
    it('성공: 사진 정보와 파일 경로를 DB에 저장하고 201 응답을 반환한다', async () => {
      // given: DB가 성공적으로 응답한다고 가정
      const mockDbResult = { insertId: 10 }; // DB 삽입 성공 시 반환값 모킹
      db.query.mockResolvedValue([mockDbResult]); // db.query가 이 값을 반환하도록 설정

      // when: 컨트롤러 함수 실행
      await photosController.createPhoto(mockRequest, mockResponse);

      // then:
      // 1. DB 쿼리가 올바른 SQL과 파라미터로 호출되었는가?
      expect(db.query).toHaveBeenCalledTimes(1);
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('INSERT INTO photos'), // SQL에 "INSERT INTO photos" 포함 확인
        [
          mockRequest.user.id,
          mockRequest.body.title,
          mockRequest.body.description,
          mockRequest.file.path // 파일 경로
        ]
      );

      // 2. 클라이언트에게 올바른 응답(201)을 보냈는가?
      expect(mockResponse.status).toHaveBeenCalledWith(201);
      expect(mockResponse.json).toHaveBeenCalledWith({
        message: '사진이 성공적으로 업로드되었습니다.',
        photoId: 10
      });
    });

    // --- 실패 케이스 (DB 에러) ---
    it('실패: DB 쿼리 중 에러가 발생하면 500 응답을 반환한다', async () => {
      // given: DB가 에러를 발생시킨다고 가정
      const errorMessage = 'DB connection error';
      db.query.mockRejectedValue(new Error(errorMessage));

      // when: 컨트롤러 함수 실행
      await photosController.createPhoto(mockRequest, mockResponse);

      // then:
      // 1. DB 쿼리가 호출되었는가? (호출은 됐지만 실패)
      expect(db.query).toHaveBeenCalledTimes(1);

      // 2. 클라이언트에게 500 에러 응답을 보냈는가?
      expect(mockResponse.status).toHaveBeenCalledWith(500);
      expect(mockResponse.json).toHaveBeenCalledWith({
        message: '서버 오류가 발생했습니다.',
        error: errorMessage
      });
    });

    // --- 실패 케이스 (입력값 누락) ---
    it('실패: req.file (파일)이 없으면 400 응답을 반환한다', async () => {
      // given: 파일이 누락된 요청
      mockRequest.file = undefined; 

      // when: 컨트롤러 함수 실행
      await photosController.createPhoto(mockRequest, mockResponse);

      // then:
      // 1. DB 쿼리는 *호출되지 않아야* 한다.
      expect(db.query).not.toHaveBeenCalled();

      // 2. 클라이언트에게 400 에러 응답을 보냈는가?
      expect(mockResponse.status).toHaveBeenCalledWith(400);
      expect(mockResponse.json).toHaveBeenCalledWith({
        message: '사진 파일이 필요합니다.'
      });
    });
  });

  // --------------------------------------------------
  //  GET /photos (사진 조회) 테스트 (추가 예시)
  // --------------------------------------------------
  describe('getPhotosByUser', () => {
    // ... (위와 유사하게 mockRequest, mockResponse 설정) ...

    it('성공: 특정 사용자의 사진 목록을 DB에서 가져와 200 응답을 반환한다', async () => {
      // given: DB가 사진 배열을 반환한다고 가정
      const mockPhotos = [
        { id: 1, title: '사진1', photo_url: 'url1' },
        { id: 2, title: '사진2', photo_url: 'url2' }
      ];
      db.query.mockResolvedValue([mockPhotos]); // DB SELECT 결과는 보통 배열 안에 배열

      const mockRequest = { user: { id: 1 } };
      const mockResponse = {
          status: jest.fn(() => mockResponse),
          json: jest.fn()
      };

      // when:
      await photosController.getPhotosByUser(mockRequest, mockResponse);

      // then:
      // 1. 올바른 SQL (user_id 기준)로 쿼리했는가?
      expect(db.query).toHaveBeenCalledWith(
        expect.stringContaining('SELECT * FROM photos WHERE user_id'), // "SELECT ... WHERE user_id" 포함 확인
        [1] // 사용자 ID
      );

      // 2. 200 응답과 함께 사진 목록을 반환했는가?
      expect(mockResponse.status).toHaveBeenCalledWith(200);
      expect(mockResponse.json(mockPhotos));
    });
  });
});