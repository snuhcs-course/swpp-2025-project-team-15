const express = require('express');
const router = express.Router();

// Import feature-specific routers
const authRoutes = require('./auth');
const diaryRoutes = require('./diary');
const aiRoutes = require('./ai');

// Route all requests to their respective routers
router.use('/auth', authRoutes);   // Endpoint: /api/auth/...
// router.use('/diary', diaryRoutes); // Endpoint: /api/diary/...
router.use('/ai', aiRoutes);       // Endpoint: /api/ai/...

module.exports = router;
