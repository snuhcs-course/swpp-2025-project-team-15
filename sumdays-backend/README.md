### 실행 방법
```$ npm install``` 을 실행하면 패키지 자동 설치됩니다
  
```$ node app.js``` 로 중앙 nodejs 서버 실행합니다. port 3000

```$ python app.py``` 로 python ai 서버 실행합니다. port 5001

```$ docker run --name my-mysql -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=diarydb -p 3306:3306 -d mysql:8``` 로 로컬에서 db 테스트
  
db 내용 확인은 아래와 같이 하면 됨.
```
docker exec -it my-mysql mysql -u root -p
(1234 입력)
SHOW DATABASES;
USE diarydb;
SHOW TABLES;
```
  
현재 openai api key는 Tutorial 5 실습용 -> 안 되면 밑의 팀 api key로 바꾸어 사용
  
### backend 구조(아직 합의 안 했는데 아마도?)
```
[Client]
   ↓
[ Node.js 서버 ]  ←→  [ MySQL(DB) ]
     ↓
[ Python 서버 (ai) ] 

```
  
### ai 관련 TODO list
  
- [ ] 텍스트 메모 합치기(-> 메모)
- [ ] 텍스트 메모 한 번에 합치기(-> 일기)
- [ ] 일기에서 이모지, 감정 점수, 피드백, 요약 추출
- [ ] 일주일 일기로 주간 요약/분석
- [ ] 한 달 일기로 월간 요약/분석
- [ ] 사진-> text (ocr)/ 일기 입력 관련이므로 날짜 추출까지.
- [ ] 음성-> text (stt)
- [ ] 사진에서 데이터 얻기(e.g. 강변에서 산책하는 사진이다)

- [ ] 추측 강도에 따른 merge로 확장
- [ ] 일기 스타일 추출
- [ ] 일기 스타일에 따른 일기 생성으로 확장
- [ ] 사진, 음성 메모에 대한 일기 생성으로 확장
