const express = require('express');
const router = express.Router();
const photosController = require('../../controllers/photosController');

// ✅ 1️⃣ 사진 생성
router.post('/:date/photos', photosController.createPhoto);

// ✅ 2️⃣ 사진 삭제
router.delete('/:date/photos/:inner_id', photosController.deletePhoto);

// ✅ 3️⃣ 사진 순서 변경
router.patch('/:date/photos/order', photosController.reorderPhotos);

module.exports = router;
