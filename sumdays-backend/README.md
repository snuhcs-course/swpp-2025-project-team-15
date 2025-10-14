### 서버 실행 방법
```$ npm install``` 을 실행하면 패키지 자동 설치됩니다  
```$ pip install -r requirements.txt``` 를 ai 폴더에서 실행하면 필요한 라이브러리가 설치됩니다. 저는 실습 때 사용한 django 용 conda 가상환경에서 필요할 때마다 설치해서 리스트를 직접 작성했다보니 누락이 있을 수 있습니다.
```$ node app.js``` 로 중앙 nodejs 서버 실행합니다.(sumdays-backend 폴더에서, port 3000)  
```$ python app.py``` 로 python ai 서버 실행합니다.(sumdays-backend/ai폴더에서, port 5001)  

### 테스트 db 환경(일단 무시)
```$ docker run --name my-mysql -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=diarydb -p 3306:3306 -d mysql:8``` 로 로컬에서 db 테스트  db 내용 확인은 아래와 같이 하면 됨.  
```docker exec -it my-mysql mysql -u root -p(1234 입력)SHOW DATABASES;USE diarydb;SHOW TABLES;```  

### .env 설정
.env.example 파일의 내용을 복사해서 .env 파일을 sumdays-backend폴더에 만들어주세요. Openai api key의 값을 바꿔서 사용해주시면 됩니다.  (공지 문서 확인)
  
우선 openai api key는 Tutorial 5 실습용 을 사용하고, 안 되면 팀 api key로 바꾸어 사용하면 크레딧 아낄 수 있을 것 같네요.  

### backend 구조
```
[Client]  
 ↓
[ Node.js 서버 ]  ←→  [ MySQL(DB) ]     
↓
[ Python 서버 (ai) ] 
```  
ai 관련 서버는 python의 flask를 사용해서 구현했습니다. Django대신 flask 사용한 이유는 제 생각에는 더 직관적이고, 메인 서버가 아니기 때문에 가벼운 기능만 있으면 되기 때문입니다.

#### 파이썬 서버
프로젝트 구조는 api endpoint에 대응하게 만들었습니다. 
http://localhost:5001/{feature_group}/{feature_name}  
-> services/{feature_group}/ 폴더  
-> 폴더 내부 routes.py에서 feature_name에 대해 구분하여 함수 연결  
-> {feature}_service.py에서 실제 기능 코드
  
구체적으로 어떻게 구현되었고, 앞으로 어떤 식으로 구현할 지는 analysis/diary 부분 구현해둔 코드 참고하면 좋을 것 같습니다. 
  
#### nodejs 서버와 연결
http://localhost:3000/api/ai/{feature_group}/{feature_name}  

app.js -> routes/index.js -> routes/ai.js -> controllers/ai/{feature_group}Controller.js  

다음의 순서대로 코드 확인하시면 어떻게 작동하는지 알 수 있습니다. analyzeController.js  의 analyze 부분 확인하시면 두 서버를 어떻게 연결하는지 알 수 있습니다.

### ai api
```
// api list - prefix: api/ai
router.post('/merge', mergeController.merge); // merge two memos
router.post('/merge-batch', mergeController.mergeBatch); // merge whole memos

router.post('/analyze', analyzeController.analyze); // analyze a diary: summary, emotion-score, emoji, feedback
router.post('/summarize-week', analyzeController.summarizeWeek); // summarize week
router.post('/summarize-month', analyzeController.summarizeMonth); // summarize month

router.post('/ocr/memo', ocrController.memo); // image memo to text
router.post('/ocr/diary', ocrController.diary); // image diary to text(must extract date)
router.post('/stt/memo', sttController.memo); // voice memo to text

router.post('/extract-style', extractController.extractStyle); // extract diary style from image/db diaries
```

### ai 관련 TODO list  
- [ ] 텍스트 메모 합치기(-> 메모)
- [ ] 텍스트 메모 한 번에 합치기(-> 일기)
- [x] 일기에서 이모지, 감정 점수, 피드백, 요약 분석
- [ ] 일주일 일기로 주간 요약/분석
- [ ] 한 달 일기로 월간 요약/분석
- [ ] 사진-> text (ocr)/ 일기 입력 관련이므로 날짜 추출까지.
- [ ] 음성-> text (stt)
- [ ] 사진에서 데이터 얻기(e.g. 강변에서 산책하는 사진이다)
- [ ] 추측 강도에 따른 merge로 확장
- [ ] 일기 스타일 추출
- [ ] 일기 스타일에 따른 일기 생성으로 확장
- [ ] 사진, 음성 메모에 대한 일기 생성으로 확장
