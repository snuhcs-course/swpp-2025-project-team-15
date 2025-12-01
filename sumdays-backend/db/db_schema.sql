-- 🧹 기존 테이블 전부 삭제 (외래키 무시)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS user_style;
DROP TABLE IF EXISTS week_summary;
DROP TABLE IF EXISTS memo;
DROP TABLE IF EXISTS daily_entry;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- 👤 users 테이블
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50) UNIQUE NOT NULL
);

-- 📔 daily_entry 테이블
CREATE TABLE daily_entry (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  date VARCHAR(50) NOT NULL,
  diary TEXT,
  keywords TEXT,
  aiComment TEXT,
  emotionScore FLOAT,
  emotionIcon TEXT,
  themeIcon TEXT,
  photoUrls Text,
  UNIQUE KEY unique_user_date (user_id, date),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 🗒 memo 테이블
CREATE TABLE memo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  room_id INT NOT NULL,
  content TEXT,
  timestamp VARCHAR(255),
  date VARCHAR(50),
  memo_order INT,
  type VARCHAR(20),
  UNIQUE KEY unique_user_room (user_id, room_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 🎨 user_style 테이블
CREATE TABLE user_style (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  styleId INT NOT NULL,
  styleName VARCHAR(255) NOT NULL,
  styleVector JSON,
  styleExamples JSON,
  stylePrompt JSON,
  sampleDiary VARCHAR(255), 
  UNIQUE KEY unique_user_style (user_id, styleId),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 📆 week_summary 테이블
CREATE TABLE week_summary (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  startDate VARCHAR(50) NOT NULL,
  endDate VARCHAR(50),
  diaryCount INT,
  emotionAnalysis JSON,
  highlights JSON,
  insights JSON,
  summary JSON,
  UNIQUE KEY unique_user_week (user_id, startDate),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);