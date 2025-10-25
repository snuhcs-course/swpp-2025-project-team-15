const mysql = require('mysql2/promise');
const { dbConfig } = require('./db');  // ✅ config 재사용

async function initializeDatabase() {
  const pool = mysql.createPool(dbConfig);

  try {
    const connection = await pool.getConnection();
    console.log('✅ RDS 연결 성공');
    console.log('⚠️ 기존 테이블을 모두 삭제하는 중...');

    // 외래키 제약 비활성화
    await connection.query('SET FOREIGN_KEY_CHECKS = 0;');

    // 테이블 드롭 (존재 여부 무관)
    await connection.query('DROP TABLE IF EXISTS photos;');
    await connection.query('DROP TABLE IF EXISTS memos;');
    await connection.query('DROP TABLE IF EXISTS daily_entries;');
    await connection.query('DROP TABLE IF EXISTS users;');

     // 외래키 제약 다시 활성화
    await connection.query('SET FOREIGN_KEY_CHECKS = 1;');
    console.log('🧹 기존 테이블 전부 삭제 완료');

    console.log('🧱 새로운 테이블을 생성하는 중...');

     // 1️⃣ users 테이블
    await connection.query(`
      CREATE TABLE users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        nickname VARCHAR(50) UNIQUE NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
      );
    `);

    // 2️⃣ daily_entries 테이블
    await connection.query(`
      CREATE TABLE daily_entries (
        id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        entry_date DATE NOT NULL,
        icon_name VARCHAR(255),
        diary TEXT,
        ai_comment TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      );
    `);

    // 3️⃣ memos 테이블
    await connection.query(`
      CREATE TABLE memos (
        id INT AUTO_INCREMENT PRIMARY KEY,
        inner_id INT NOT NULL,
        memo_order INT NOT NULL,
        daily_entry_id INT NOT NULL,
        content TEXT,
        memo_time TIME,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (daily_entry_id) REFERENCES daily_entries(id) ON DELETE CASCADE
      );
    `);

    // 4️⃣ photos 테이블
    await connection.query(`
      CREATE TABLE photos (
        id INT AUTO_INCREMENT PRIMARY KEY,
        inner_id INT NOT NULL,
        daily_entry_id INT NOT NULL,
        photo_order INT NOT NULL,
        extension VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (daily_entry_id) REFERENCES daily_entries(id) ON DELETE CASCADE
      );
    `);

    console.log('✅ 모든 테이블 생성 완료');
    connection.release();
    await pool.end();
  } catch (err) {
    console.error('❌ 데이터베이스 초기화 실패:', err.message);
  }
}

// ▶ 직접 실행
(async () => {
  console.log('🛠 데이터베이스 초기화 중...');
  await initializeDatabase();
  console.log('✅ 초기화 완료!');
  process.exit(0);
})();


