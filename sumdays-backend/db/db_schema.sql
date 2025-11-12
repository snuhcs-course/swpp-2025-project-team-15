
Table users {
  id Integer [pk, increment]
  email varchar(255) [unique, not null]
  password_hash varchar(255) [not null]
  nickname varchar(50) [unique, not null]
}

Table daily_entry {
  id            int          [pk, increment]     // 서버 내부 PK
  user_id       int          [not null]          // 유저 ID
  date          varchar(50)  [not null]          // Room의 PrimaryKey (yyyy-MM-dd)
  diary         text                             // 일기 내용
  keywords      text                             // 키워드 (;로 구분)
  aiComment     text                             // AI 코멘트
  emotionScore  float                             // 감정 점수
  emotionIcon   text                     // 감정 이모지
  themeIcon     text                    // 테마 이모지

  Indexes {
    unique (user_id, date) // ✅ 유저별 일자 중복 방지
  }
}

Table memo {
  id           int          [pk, increment]     // 서버 내부 PK
  user_id      int                    // 유저 ID
  room_id      int                    // Room DB의 id
  content      text                   // 메모 내용
  timestamp    varchar(255)           // 작성 시각
  date         varchar(50)            // 날짜
  memo_order   int                    // 메모 순서
  type         varchar(20)     // 메모 타입

  Indexes {
    unique (user_id, room_id) // 유저별 고유 Room ID
  }
}

Table week_summary {
  id              int          [pk, increment]     // 서버 내부 PK
  user_id         int          [not null]          // 유저 ID
  startDate       varchar(50)  [not null]          // 주 시작일 (유일키)
  endDate         varchar(50)                      // 주 종료일
  diaryCount      int                              // 작성된 일기 수
  emotionAnalysis json                             // 감정 분석 (EmotionAnalysis)
  highlights      json                             // 핵심 일기 목록 (List<Highlight>)
  insights        json                             // 통찰 / 조언 (Insights)
  summary         json                             // 주간 요약 개요 (SummaryDetails)

  Indexes {
    unique (user_id, startDate) // ✅ 유저별 주간 요약 중복 방지
  }
}


Table user_style {
  id             int          [pk, increment]     // 서버 내부 PK
  user_id        int          [not null]          // 유저 ID
  styleId        int          [not null]          // Room DB의 styleId
  styleName      varchar(255) [not null]          // 스타일 이름
  styleVector    json                             // 벡터 (List<Float>)
  styleExamples  json                             // 예시 문장들 (List<String>)
  stylePrompt    json                             // 분석 프롬프트 (StylePrompt 객체)

  Indexes {
    unique (user_id, styleId) // ✅ 유저별 스타일 ID 중복 방지
  }
}
