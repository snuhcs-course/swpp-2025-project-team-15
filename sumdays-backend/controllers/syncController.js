const db = require('../db/db');
const path = require("path");
const process = require("process");
const fs = require("fs");


// 1. local to server
exports.syncData = async (req, res) => {
  try {
    const {deleted, edited } = req.body;
    const userId = req.user.userId

    // ✅ 1️⃣ 업데이트 / 신규 생성 데이터 반영
    if (updated) {

    }

    // ✅ 2️⃣ 삭제 로그 반영
    if (deleted) [
        
    ]

    res.json({
      status: 'success',
      message: 'Sync completed successfully.',
    });
  } catch (error) {
    console.error('[syncController] Error:', error);
    res.status(500).json({ status: 'error', message: error.message });
  }
};

// 2. server to local (아직 코드 미완성)
exports.fetchServerData = async (req, res) => {
  try {
    const { user_id } = req.query;
    const [memos] = await db.query(`SELECT * FROM memo_table WHERE user_id = ?`, [user_id]);
    const [weeks] = await db.query(`SELECT * FROM week_summary WHERE user_id = ?`, [user_id]);

    res.json({
      status: 'success',
      memos,
      weekSummary: weeks,
    });
  } catch (error) {
    console.error('[fetchServerData] Error:', error);
    res.status(500).json({ status: 'error', message: error.message });
  }
};