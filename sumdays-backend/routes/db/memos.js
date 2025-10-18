const express = require('express');
const router = express.Router();
const memosController = require('../../controllers/memosController');

// ✅ 1️⃣ 메모 생성
router.post('/:date/memos', memosController.createMemo);

// ✅ 2️⃣ 메모 수정
router.patch('/:date/memos/:inner_id', memosController.updateMemo);

// ✅ 3️⃣ 메모 삭제
router.delete('/:date/memos/:inner_id', memosController.deleteMemo);

// ✅ 4️⃣ 메모 순서 변경
router.patch('/:date/memos/order', memosController.reorderMemos);

module.exports = router;
