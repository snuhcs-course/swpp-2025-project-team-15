-- =======================================================
-- Sumdays Project Database Schema
-- =======================================================
-- 각 테이블을 만들기 전에 안전하게 초기화
DROP TABLE IF EXISTS memos;
DROP TABLE IF EXISTS daily_entries;
DROP TABLE IF EXISTS user_stats;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

-- =======================================================
-- USERS
-- =======================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
);

-- =======================================================
-- USER PROFILES
-- =======================================================
CREATE TABLE user_profiles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    nickname VARCHAR(255),
    bio TEXT,
    profile_image_url VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =======================================================
-- USER STATS
-- =======================================================
CREATE TABLE user_stats (
    user_id INT PRIMARY KEY,
    total_entries INT DEFAULT 0,
    writing_streak INT DEFAULT 0,
    most_used_icon VARCHAR(255),
    last_entry_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =======================================================
-- DAILY ENTRIES
-- =======================================================
CREATE TABLE daily_entries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    entry_date DATE NOT NULL,
    icon VARCHAR(255),
    diary TEXT,
    ai_comment TEXT,
    photo_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =======================================================
-- MEMOS
-- =======================================================
CREATE TABLE memos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    daily_entry_id INT NOT NULL,
    content TEXT NOT NULL,
    memo_time TIME,
    `order` INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (daily_entry_id) REFERENCES daily_entries(id) ON DELETE CASCADE
);
