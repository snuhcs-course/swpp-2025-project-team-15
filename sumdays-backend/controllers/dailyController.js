const db = require('../db/db');

/* -------------------------------------------------------------------------- */
/*  1️⃣ GET /api/db/daily/:date — 일기 조회 (전체 / include / exclude 지원)   */
/* -------------------------------------------------------------------------- */
exports.getDailyEntry = async (req, res) => {
  const { date } = req.params;
  const { include, exclude } = req.query;

  try {
    const [entries] = await db.query(
      'SELECT * FROM daily_entries WHERE entry_date = ?',
      [date]
    );

    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const entry = entries[0];

    const [memos] = await db.query(
      'SELECT inner_id, `order`, content, memo_time FROM memos WHERE daily_entry_id = ? ORDER BY `order` ASC',
      [entry.id]
    );

    const [photos] = await db.query(
      'SELECT inner_id, `order`, base64Image FROM photos WHERE daily_entry_id = ? ORDER BY `order` ASC',
      [entry.id]
    );

    let data = {
      date: entry.entry_date,
      icon_name: entry.icon_name || "",
      diary: entry.diary || "",
      ai_comment: entry.ai_comment || "",
      memos: memos || [],
      photos: photos || []
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
/*  2️⃣ POST /api/db/daily — 새 일기 생성                                      */
/* -------------------------------------------------------------------------- */
exports.createDailyEntry = async (req, res) => {
  const { date } = req.body;

  try {
    const [exists] = await db.query(
      'SELECT id FROM daily_entries WHERE entry_date = ?',
      [date]
    );
    if (exists.length > 0) {
      return res.status(409).json({ message: 'Entry already exists for this date' });
    }

    await db.query(
      'INSERT INTO daily_entries (entry_date, created_at, updated_at) VALUES (?, NOW(), NOW())',
      [date]
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

  try {
    const [entries] = await db.query(
      'SELECT id FROM daily_entries WHERE entry_date = ?',
      [date]
    );
    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

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

    values.push(date);
    const sql = `UPDATE daily_entries SET ${updates.join(', ')}, updated_at = NOW() WHERE entry_date = ?`;
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

  try {
    const [entries] = await db.query(
      'SELECT id FROM daily_entries WHERE entry_date = ?',
      [date]
    );
    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const entryId = entries[0].id;

    await db.query('DELETE FROM memos WHERE daily_entry_id = ?', [entryId]);
    await db.query('DELETE FROM photos WHERE daily_entry_id = ?', [entryId]);
    await db.query('DELETE FROM daily_entries WHERE id = ?', [entryId]);

    res.status(200).json({ message: 'Daily entry deleted successfully' });
  } catch (error) {
    console.error('❌ [deleteDailyEntry] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};
