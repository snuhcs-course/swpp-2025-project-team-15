const express = require('express');
const router = express.Router();

// 세부 라우터 import
const authRoutes = require('./auth');
const diaryRoutes = require('./diary');
const aiRoutes = require('./ai');

// 각각의 경로에 연결
router.use('/auth', authRoutes);   // /api/auth/...
router.use('/diary', diaryRoutes); // /api/diary/...
router.use('/ai', aiRoutes);       // /api/ai/...

module.exports = router;
