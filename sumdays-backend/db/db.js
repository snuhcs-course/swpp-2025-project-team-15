const mysql = require('mysql2/promise'); // MySQL 데이터베이스와 비동기 통신을 위한 라이브러리

const dbConfig = {
    host: 'sumdays-database.c502cecwedjd.ap-northeast-2.rds.amazonaws.com',      // 예: sumdays-db.abcdefg12345.ap-northeast-2.rds.amazonaws.com
    user: 'swpp_team15',       // 예: admin
    password: 'aoij*i9!jUjkm',   // RDS 생성 시 설정한 마스터 암호
    database: 'login',         // 연결할 데이터베이스 이름
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

// 데이터베이스 커넥션 풀(Pool) 생성. 여러 요청을 효율적으로 처리합니다.
const pool = mysql.createPool(dbConfig);
const post_pool = mysql.createPool(dbConfig);