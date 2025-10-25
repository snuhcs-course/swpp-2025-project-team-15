// middlewares/authMiddleware.js

module.exports = async function verifyToken(req, res, next) {
  const jwt = require('jsonwebtoken');
  const db = require('../db/db'); 
  const JWT_SECRET = process.env.JWT_SECRET || 'Sumdays_Project_Super_Secret_Key_!@#$%^&*()';


  const authHeader = req.headers['authorization'];

  // 헤더가 없으면 401
  if (!authHeader) {
    return res.status(401).json({ message: '토큰이 없습니다.' });
  }

  // "Bearer <token>" → 실제 토큰만 분리
  const token = authHeader.split(' ')[1];

  try {
    // 토큰 검증
    const decoded = jwt.verify(token, JWT_SECRET);
    
    // 해당 회원 존재하는지
    const [rows] = await db.query('SELECT id FROM users WHERE id = ?', [decoded.userId]);
    if (rows.length === 0) {
      return res.status(401).json({ message: '존재하지 않는 사용자입니다.' });
    }

    // 토큰에 담긴 사용자 정보(req.user에 저장)
    req.user = decoded;
    // 다음 미들웨어 또는 컨트롤러로 이동
    next();
  } catch (err) {
    console.error('❌ [JWT 검증 실패]', err);
    return res.status(403).json({ message: '유효하지 않은 토큰입니다.' });
  }
};
