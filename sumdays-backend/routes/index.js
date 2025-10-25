const express = require('express');
const router = express.Router();
const verifyToken = require('../middlewares/authMiddleware');

// 세부 라우터 import
const authRoutes = require('./auth');
const aiRoutes = require('./ai');
const dailyRoutes = require('./db/daily');
const memosRoutes = require('./db/memos');
const photosRoutes = require('./db/photos');


router.use('/auth', authRoutes);   // /api/auth/...
router.use('/ai', aiRoutes);       // /api/ai/...

router.use('/db', verifyToken);
router.use('/db/daily', dailyRoutes);        
router.use('/db/daily/memos', memosRoutes);       
router.use('/db/daily/photos', photosRoutes);  

module.exports = router;
