// 1. 필요한 라이브러리들을 가져옵니다.
const express = require('express');

// 2. Express 애플리케이션을 생성합니다.
const app = express();
const port = 3000; // 안드로이드 앱의 RetrofitClient와 동일한 포트 번호

// 3. Middlewares 설정
// express.json()은 들어오는 요청의 본문(body)을 JSON으로 파싱해줍니다.
app.use(express.json());

// 4. API 라우트(Route) 정의
const routes = require('./routes');
app.use('/api', routes);


// 5. 서버 실행
app.listen(port, () => {
    console.log(`Sumdays 백엔드 서버가 포트 ${port}번에서 실행 중입니다.`);
    console.log('안드로이드 앱의 요청을 기다리고 있습니다...');
});
