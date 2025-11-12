const db = require('../db/db');
const path = require("path");
const process = require("process");
const fs = require("fs");


// 1. local to server
exports.syncData = async (req, res) => {
  try {
    const {deleted, edited } = req.body;
    const userId = req.user.userId

     /** ------------------------------
     * 🧩 공통 INSERT or UPDATE 함수
     * ------------------------------ */
    const upsert = async (table, data, columns) => {
      if (!Array.isArray(data) || data.length === 0) return;

      // INSERT INTO ... ON DUPLICATE KEY UPDATE ...
      const fields = columns.join(', ');
      const placeholders = columns.map(() => '?').join(', ');
      const updates = columns.map(col => `${col}=VALUES(${col})`).join(', ');

      const sql = `
        INSERT INTO ${table} (user_id, ${fields})
        VALUES ${data.map(() => `(?, ${placeholders})`).join(', ')}
        ON DUPLICATE KEY UPDATE ${updates};
      `;

      // 각 데이터 → 파라미터로 flatten
      const values = data.flatMap(item => [
        userId,
        ...columns.map(c => item[c])
      ]);

      await db.query(sql, values);
      console.log(`✅ Upserted ${data.length} rows into ${table}`);
    };

    // -------------------------------------------------------------------------------
    
    // 1. 삭제 data
    if (deleted) {
        const deleteIfExists = async (table, keyField, ids) => {
            if (!Array.isArray(ids) || ids.length === 0) return;
            const placeholders = ids.map(() => '?').join(',');
            const sql = `DELETE FROM ${table} WHERE ${keyField} IN (${placeholders}) AND user_id = ?`;
            await db.query(sql, [...ids, userId]);
            console.log(`✅ Deleted from ${table}: ${ids.length} rows`);
        };

    // 🧱 각 테이블별 삭제 반영
        if (deleted.memo) {
            await deleteIfExists('memo', 'room_id', deleted.memo);
        }
        if (deleted.dailyEntry) {
            await deleteIfExists('daily_entry', 'date', deleted.dailyEntry);
        }
        if (deleted.userStyle) {
            await deleteIfExists('user_style', 'styleId', deleted.userStyle);
        }
        if (deleted.weekSummary) {
            await deleteIfExists('week_summary', 'startDate', deleted.weekSummary);
        }
    }
    // 2. 추가, 수정 data 
    if (edited) {
      if (edited.memo) {
        await upsert(
          'memo',
          edited.memo,
          ['user_id', 'room_id'],
          ['room_id', 'content', 'timestamp', 'date', 'memo_order', 'type']
        );
      }

      if (edited.dailyEntry) {
        await upsert(
          'daily_entry',
          edited.dailyEntry,
          ['user_id', 'date'],
          ['date', 'diary', 'keywords', 'aiComment', 'emotionScore', 'emotionIcon', 'themeIcon']
        );
      }

      if (edited.weekSummary) {
        await upsert(
          'week_summary',
          edited.weekSummary,
          ['user_id', 'startDate'],
          ['startDate', 'endDate', 'diaryCount', 'emotionAnalysis', 'highlights', 'insights', 'summary']
        );
      }

      if (edited.userStyle) {
        await upsert(
          'user_style',
          edited.userStyle,
          ['user_id', 'styleId'],
          ['styleId', 'styleName', 'styleVector', 'styleExamples', 'stylePrompt']
        );
      }
    }



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