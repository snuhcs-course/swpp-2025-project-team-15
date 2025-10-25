const db = require('../db/db');
const path = require("path");
const process = require("process");
const fs = require("fs");
/* -------------------------------------------------------------------------- */
/*  GET /api/db/daily/:date (Total / include / exclude) */
/* -------------------------------------------------------------------------- */
exports.getDailyEntry = async (req, res) => {
  const { date } = req.params;
  const { include, exclude } = req.query;
  const userId = req.user.userId

  try {
    // 1. 기본 entries 
    const [entries] = await db.query(
      'SELECT * FROM daily_entries WHERE user_id = ? AND entry_date = ?',
      [userId,date]
    );

    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }
    const entry = entries[0];


    // 2. Memos + Photos
    const [memos] = await db.query(
      'SELECT inner_id, memo_order, content, memo_time FROM memos WHERE daily_entry_id = ? ORDER BY memo_order ASC',
      [entry.id]
    );
    const [photos] = await db.query(
      'SELECT inner_id, photo_order, extension FROM photos WHERE daily_entry_id = ? ORDER BY photo_order ASC',
      [entry.id]
    )
    const photoData = photos.map((photo) => {    
      const filePath = path.join(
        process.cwd(),
        "uploads",
        entry.user_id.toString(),
        entry.entry_date,
        `${photo.inner_id}.${photo.extension}`
      );

      // 존재 x => null
      if (!fs.existsSync(filePath)) {
        return {
          inner_id: photo.inner_id,
          order: photo.photo_order,
          base64: null
        };
      }

      // 파일을 base64로 변환
      const imageBuffer = fs.readFileSync(filePath);
      const base64String = imageBuffer.toString("base64");
      // 변환된 데이터 반환
      return {
        inner_id: photo.inner_id,
        order: photo.photo_order,
        base64: `data:image/${photo.extension};base64,${base64String}`
      };
    });


    let data = {
      date: entry.entry_date,
      icon_name: entry.icon_name,
      diary: entry.diary,
      ai_comment: entry.ai_comment,
      memos: memos,
      photos: photoDatas
    };

    // include / exclude 필드 처리
    if (include) {
      const includeFields = include.split(',').map(f => f.trim());
      data = Object.fromEntries(
        Object.entries(data).filter(([key]) => includeFields.includes(key))
      );
    }
    if (exclude) {
      const excludeFields = exclude.split(',').map(f => f.trim());
      data = Object.fromEntries(
        Object.entries(data).filter(([key]) => !excludeFields.includes(key))
      );
    }

    res.status(200).json(data);
  } catch (error) {
    console.error('❌ [getDailyEntry] Error:', error);
    res.status(500).json({ error: 'Database query failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  POST /api/db/daily — new daily entry */                                     
/* -------------------------------------------------------------------------- */
exports.createDailyEntry = async (req, res) => {
  const { date } = req.body;
  const userId = req.user.userId

  try {
    const [exists] = await db.query(
      'SELECT id FROM daily_entries WHERE user_id = ? AND entry_date = ?',
      [userId, date]
    );
    if (exists.length > 0) {
      return res.status(409).json({ message: 'Entry already exists for this date' });
    }

    await db.query(
      'INSERT INTO daily_entries (user_id, entry_date) VALUES (?, ?)',
      [userId, date]
    );

    res.status(201).json({ message: 'Daily entry created successfully' });
  } catch (error) {
    console.error('❌ [createDailyEntry] Error:', error);
    res.status(500).json({ error: 'Database insert failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  3️⃣ PATCH /api/db/daily/:date — 일기 수정 (icon_name, diary, ai_comment)   */
/* -------------------------------------------------------------------------- */
exports.updateDailyEntry = async (req, res) => {
  const { date } = req.params;
  const { icon_name, diary, ai_comment } = req.body;
  const userId = req.user.userId

  try {
    // 1. find dailyEntry
    const [entries] = await db.query(
      'SELECT id FROM daily_entries WHERE user_id = ? AND entry_date = ?',
      [userId, date]
    );
    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    // 2. make query (icon_name, diary, ai_comment)
    const updates = [];
    const values = [];

    if (icon_name !== undefined) {
      updates.push('icon_name = ?');
      values.push(icon_name);
    }
    if (diary !== undefined) {
      updates.push('diary = ?');
      values.push(diary);
    }
    if (ai_comment !== undefined) {
      updates.push('ai_comment = ?');
      values.push(ai_comment);
    }

    if (updates.length === 0) {
      return res.status(400).json({ message: 'No valid fields provided for update' });
    }

    values.push(userId, date);
    const sql = `UPDATE daily_entries SET ${updates.join(', ')} WHERE user_id = ? AND entry_date = ?`;
    await db.query(sql, values);

    res.status(200).json({ message: 'Daily entry updated successfully' });
  } catch (error) {
    console.error('❌ [updateDailyEntry] Error:', error);
    res.status(500).json({ error: 'Database update failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  4️⃣ DELETE /api/db/daily/:date — 일기 삭제                                 */
/* -------------------------------------------------------------------------- */
exports.deleteDailyEntry = async (req, res) => {
  const { date } = req.params;
  const userId = req.user.userId 
  
  try {
    await db.query('DELETE FROM daily_entries WHERE user_id = ? AND entry_date = ?', [userId,date]);

    res.status(200).json({ message: 'Daily entry deleted successfully' });
  } catch (error) {
    console.error('❌ [deleteDailyEntry] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};
