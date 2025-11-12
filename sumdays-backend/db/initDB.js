const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
const { dbConfig } = require('./db');

async function initializeDatabase() {
  const pool = require('./db');
  

  try {
    const sqlPath = path.join(__dirname, 'db_schema.sql');
    const schemaSQL = fs.readFileSync(sqlPath, 'utf8');

    const connection = await pool.getConnection();
    console.log('✅ RDS 연결 성공, 스키마 초기화 중...');
    
    // 여러 쿼리 한 번에 실행
    await connection.query(schemaSQL);
    console.log('✅ DB 스키마 적용 완료');

    connection.release();
    await pool.end();
  } catch (err) {
    console.error('❌ DB 초기화 실패:', err.message);
  }
}

(async () => {
  console.log('🛠 데이터베이스 초기화 중...');
  await initializeDatabase();
  console.log('✅ 초기화 완료!');
  process.exit(0);
})();
