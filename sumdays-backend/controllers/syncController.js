const {pool} = require('../db/db');
const path = require("path");
const process = require("process");
const fs = require("fs");


// 1. local to server
exports.syncData = async (req, res) => {
  try {
    console.log(JSON.stringify(req.body, null, 2));
    const {deleted, edited } = req.body;
    const userId = req.user.userId

    /** -----------------------------------------------------
     * 🧩 0. users 테이블에서 userId 없으면 자동 생성
     * ----------------------------------------------------- */
    const [userRows] = await pool.query(
      `SELECT id FROM users WHERE id = ?`,
      [userId]
    );

    if (userRows.length === 0) {
      console.log(`⚠ user_id=${userId} 없음 → 자동 생성합니다.`);

      await pool.query(
        `INSERT INTO users (id, email, password_hash, nickname)
         VALUES (?, ?, ?, ?)`,
        [
          userId,
          `auto_user_${userId}@example.com`,
          'auto_created_dummy_hash',
          `auto_user_${userId}`
        ]
      );
      console.log(`✅ user_id=${userId} 자동 생성 완료`);
    } else {
      console.log(`👍 user_id=${userId} 이미 존재`);
    }
    ////////////////////


     /** ------------------------------
     * 🧩 공통 INSERT or UPDATE 함수
     * ------------------------------ */
    const upsert = async (table, data, columns) => {
  if (!Array.isArray(data) || data.length === 0) return;

  const fields = columns.join(', ');
  const placeholders = columns.map(() => '?').join(', ');
  const updates = columns.map(col => `${col}=VALUES(${col})`).join(', ');

  const sql = `
    INSERT INTO ${table} (user_id, ${fields})
    VALUES ${data.map(() => `(?, ${placeholders})`).join(', ')}
    ON DUPLICATE KEY UPDATE ${updates};
  `;

  const values = data.flatMap(item => [
    userId,
    ...columns.map(c => {
      const value = item[c];
      // JSON 컬럼이면 stringify
      if (typeof value === 'object' && value !== null) {
        return JSON.stringify(value);
      }
      return value;
    })
  ]);

  await pool.query(sql, values);
  console.log(`✅ Upserted ${data.length} rows into ${table}`);
    };


    // -------------------------------------------------------------------------------
    
    // 1. 삭제 data
    if (deleted) {
        const deleteIfExists = async (table, keyField, ids) => {
            if (!Array.isArray(ids) || ids.length === 0) return;
            const placeholders = ids.map(() => '?').join(',');
            const sql = `DELETE FROM ${table} WHERE ${keyField} IN (${placeholders}) AND user_id = ?`;
          await pool.query(sql, [...ids, userId]);
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
          ['room_id', 'content', 'timestamp', 'date', 'memo_order', 'type']
        );
      }

      if (edited.dailyEntry) {
        await upsert(
          'daily_entry',
          edited.dailyEntry,
          ['date', 'diary', 'keywords', 'aiComment', 'emotionScore', 'emotionIcon', 'themeIcon']
        );
      }

      if (edited.weekSummary) {
        await upsert(
          'week_summary',
          edited.weekSummary,
          ['startDate', 'endDate', 'diaryCount', 'emotionAnalysis', 'highlights', 'insights', 'summary']
        );
      }

      if (edited.userStyle) {
        await upsert(
          'user_style',
          edited.userStyle,
          ['styleId', 'styleName', 'styleVector', 'styleExamples', 'stylePrompt', 'sampleDiary']
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


function safeParse(str, fallback) {
  try {
    return str ? JSON.parse(str) : fallback;
  } catch (e) {
    return fallback;
  }
}

// 2. server to local (아직 코드 미완성)
exports.fetchServerData = async (req, res) => {
  try {
    const userId = req.user.userId

    const [memo] = await pool.query(
      `SELECT * FROM memo WHERE user_id=?`, [userId]
    );

    const [dailyEntry] = await pool.query(
      `SELECT * FROM daily_entry WHERE user_id=?`, [userId]
    );

    const [weekRows] = await pool.query(
      `SELECT * FROM week_summary WHERE user_id=?`, [userId]
    );

    const [styleRows] = await pool.query(
      `SELECT * FROM user_style WHERE user_id=?`, [userId]
    );

    // JSON 컬럼만 parse
    const weekSummary = weekRows.map(row => ({
      ...row,
      emotionAnalysis: safeParse(row.emotionAnalysis, {}),
      highlights: safeParse(row.highlights, []),
      insights: safeParse(row.insights, {}),
      summary: safeParse(row.summary, {})
    }));

    const userStyle = styleRows.map(row => ({
      ...row,
      styleVector: safeParse(row.styleVector, []),
      styleExamples: safeParse(row.styleExamples, []),
      stylePrompt: safeParse(row.stylePrompt, {}),
      sampleDiary : row.sampleDiary
    }));


    res.json({
      memo,
      dailyEntry,
      weekSummary,
      userStyle
    });

  } catch (e) {
    res.status(500).json({ status: "error", message: e.message });
  }
};
