
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
        111,
        ...columns.map(c => item[c])
      ]);

      console.log('📘 SQL:', sql);
      console.log('📦 VALUES:', values);

      // await db.query(sql, values);
      // console.log(`✅ Upserted ${data.length} rows into ${table}`);
    };

const edited = {
  memo: [
    {
      room_id: 1,
      content: "오늘은 백업 테스트 중",
      timestamp: "2025-11-13T10:30:00",
      date: "2025-11-13",
      memo_order: 1,
      type: "text"
    },
    {
      room_id: 2,
      content: "이건 두 번째 메모",
      timestamp: "2025-11-13T10:45:00",
      date: "2025-11-13",
      memo_order: 2,
      type: "voice"
    },
    {
      room_id: 3,
      content: "세 번째 메모는 동기화 검증용입니다",
      timestamp: "2025-11-13T11:00:00",
      date: "2025-11-13",
      memo_order: 3,
      type: "text"
    }

]
};



 upsert(
          'memo',
          edited.memo,
          ['room_id', 'content', 'timestamp', 'date', 'memo_order', 'type']
        );  