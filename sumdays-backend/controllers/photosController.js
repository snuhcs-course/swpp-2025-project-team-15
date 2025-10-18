const db = require('../db/db');

/* -------------------------------------------------------------------------- */
/*  1️⃣ POST /api/db/photos/:date/photos — 사진 생성                            */
/* -------------------------------------------------------------------------- */
exports.createPhoto = async (req, res) => {
  const { date } = req.params;
  const { base64Image, inner_id, order } = req.body;

  try {
    const [entries] = await db.query('SELECT id FROM daily_entries WHERE entry_date = ?', [date]);
    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const daily_entry_id = entries[0].id;

    await db.query(
      'INSERT INTO photos (daily_entry_id, inner_id, `order`, base64Image, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())',
      [daily_entry_id, inner_id, order, base64Image]
    );

    res.status(201).json({ message: 'Photo created successfully' });
  } catch (error) {
    console.error('❌ [createPhoto] Error:', error);
    res.status(500).json({ error: 'Database insert failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  2️⃣ DELETE /api/db/photos/:date/photos/:inner_id — 사진 삭제                */
/* -------------------------------------------------------------------------- */
exports.deletePhoto = async (req, res) => {
  const { inner_id } = req.params;

  try {
    await db.query('DELETE FROM photos WHERE inner_id = ?', [inner_id]);
    res.status(200).json({ message: 'Photo deleted successfully' });
  } catch (error) {
    console.error('❌ [deletePhoto] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  3️⃣ PATCH /api/db/photos/:date/photos/order — 사진 순서 변경                */
/* -------------------------------------------------------------------------- */
exports.reorderPhotos = async (req, res) => {
  const { date } = req.params;
  const orderMap = req.body; // { "1": 2, "2": 1, "3": 3 }

  try {
    const [entries] = await db.query('SELECT id FROM daily_entries WHERE entry_date = ?', [date]);
    if (entries.length === 0) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const daily_entry_id = entries[0].id;
    const updatePromises = Object.entries(orderMap).map(([inner_id, newOrder]) =>
      db.query('UPDATE photos SET `order` = ? WHERE daily_entry_id = ? AND inner_id = ?', [newOrder, daily_entry_id, inner_id])
    );

    await Promise.all(updatePromises);
    res.status(200).json({ message: 'Photo order updated successfully' });
  } catch (error) {
    console.error('❌ [reorderPhotos] Error:', error);
    res.status(500).json({ error: 'Database update failed' });
  }
};
