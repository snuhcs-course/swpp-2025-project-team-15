// routes/auth.js
const express = require('express');
const router = express.Router();

// 로그인 / 회원가입 로직이 있는 컨트롤러
const authController = require('../controllers/authController');

// 로그인 엔드포인트
router.post('/login', authController.login);

// 회원가입 엔드포인트
router.post('/signup', authController.signup);

module.exports = router;
