const express = require('express');
const router = express.Router();

// 세부 라우터 import
const authRoutes = require('./auth');
const aiRoutes = require('./ai');
const dailyRoutes = require('./db/daily');
const memosRoutes = require('./db/memos');
const photosRoutes = require('./db/photos');

// 각각의 경로에 연결
router.use('/auth', authRoutes);   // /api/auth/...
router.use('/ai', aiRoutes);       // /api/ai/...
router.use('/db/daily', dailyRoutes);        
router.use('/db/memos', memosRoutes);       
router.use('/db/photos', photosRoutes);  

module.exports = router;
