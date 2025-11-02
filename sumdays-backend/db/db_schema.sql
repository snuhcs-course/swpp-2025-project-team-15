-- ⚠️ 모든 테이블 삭제 (FK 무시)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS photos;
DROP TABLE IF EXISTS memos;
DROP TABLE IF EXISTS daily_entries;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------
-- 1️⃣ users
-- ---------------------------
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ---------------------------
-- 2️⃣ daily_entries
-- ---------------------------
CREATE TABLE daily_entries (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  entry_date DATE NOT NULL,
  icon_name VARCHAR(255),
  diary TEXT,
  ai_comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_daily_user FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);

-- ---------------------------
-- 3️⃣ memos
-- ---------------------------
CREATE TABLE memos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  inner_id INT NOT NULL,
  memo_order INT NOT NULL,
  daily_entry_id INT NOT NULL,
  content TEXT,
  memo_time TIME,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_memo_entry FOREIGN KEY (daily_entry_id)
    REFERENCES daily_entries(id)
    ON DELETE CASCADE
);

-- ---------------------------
-- 4️⃣ photos
-- ---------------------------
CREATE TABLE photos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  inner_id INT NOT NULL,
  daily_entry_id INT NOT NULL,
  photo_order INT NOT NULL,
  extension VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_photo_entry FOREIGN KEY (daily_entry_id)
    REFERENCES daily_entries(id)
    ON DELETE CASCADE
);
