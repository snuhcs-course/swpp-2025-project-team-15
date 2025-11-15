package com.example.sumdays.settings


class StyleExtractionActivityTest {

//    // --- 1. 테스트 규칙 (Rules) ---
//
//    // Coroutine 디스패처를 제어 (Main, IO 등)
//    // (파일 하단에 정의된 중첩 클래스 MainCoroutineRule 사용)
//    @get:Rule
//    val mainCoroutineRule = MainCoroutineRule()
//
//    // ActivityResult (이미지 선택) API를 테스트하기 위한 Rule
//    @get:Rule
//    val intentsRule = Intents.testRule()
//
//    // --- 2. Mock 객체 및 테스트 데이터 ---
//
//    private lateinit var mockApiService: ApiService
//    private lateinit var scenario: ActivityScenario<StyleExtractionActivity>
//
//    // 테스트용 가짜 데이터 (파일에서 가져온 실제 클래스 사용)
//    private val fakeStylePrompt = StylePrompt(
//        common_phrases = listOf("테스트"), emotional_tone = "happy", formality = "formal",
//        irony_or_sarcasm = "none", lexical_choice = "easy", pacing = "fast",
//        sentence_endings = listOf("."), sentence_length = "short",
//        sentence_structure = "simple", slang_or_dialect = "none", tone = "bright"
//    )
//
//    // API 성공 응답 (StyleExtractionActivity의 null 체크를 통과해야 함)
//    private val fakeSuccessResponse = StyleExtractionResponse(
//        success = true,
//        style_vector = listOf(0.1f, 0.2f, 0.3f),
//        style_examples = listOf("테스트 예시 1.", "테스트 예시 2."),
//        style_prompt = fakeStylePrompt,
//        message = "Success"
//    )
//
//    // 샘플 일기 생성 API 응답
//    private val fakeSampleDiaryResponse = JsonObject().apply {
//        // extractMergedText가 파싱할 수 있는 구조
//        val result = JsonObject().apply { addProperty("diary", "생성된 샘플 일기입니다.") }
//        add("result", result)
//    }
//
//    private lateinit var db: StyleDatabase
//
//    // --- 3. 테스트 설정 및 해제 ---
//
//    @Before
//    fun setUp() {
//        // --- 의존성 모킹 (Mocking) ---
//
//        // 1. ApiService 모킹
//        mockApiService = mockk()
//        mockkObject(ApiClient) // ApiClient는 object이므로 mockkObject 사용
//        every { ApiClient.api } returns mockApiService // ApiClient.api 호출 시 mockApiService 반환
//
//        // 2. FileUtil 모킹 (실제 파일 시스템 접근 방지)
//        mockkObject(FileUtil)
//        val mockFile = mockk<File>(relaxed = true)
//        every { mockFile.exists() } returns true
//        every { mockFile.name } returns "fake_image.jpg"
//        every { FileUtil.getFileFromUri(any(), any()) } returns mockFile
//
//        // 3. MemoMergeUtils 모킹
//        mockkObject(MemoMergeUtils)
//        every { MemoMergeUtils.convertStylePromptToMap(any()) } returns emptyMap()
//        // extractMergedText의 로직을 모킹 (실제 로직 대신 가짜 응답 반환)
//        every { MemoMergeUtils.extractMergedText(fakeSampleDiaryResponse) } returns "생성된 샘플 일기입니다."
//
//        // 4. Room Database 초기화 (테스트마다 깨끗한 DB 사용)
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        db = Room.inMemoryDatabaseBuilder(context, StyleDatabase::class.java)
//            .allowMainThreadQueries() // 테스트에서는 Main 스레드 쿼리 허용
//            .build()
//
//        // StyleDatabase 싱글톤 인스턴스 교체 (중요!)
//        // getDatabase()가 항상 위에서 만든 인메모리 DB를 반환하도록 함
//        mockkObject(StyleDatabase)
//        every { StyleDatabase.getDatabase(any()) } returns db
//
//        // --- API 기본 응답 설정 (Happy Path) ---
//
//        // 1. extractStyle (Call.enqueue 방식)
//        stubExtractStyleResponse(Response.success(fakeSuccessResponse))
//
//        // 2. mergeMemos (suspend 방식)
//        coEvery { mockApiService.mergeMemos(any()) } returns Response.success(fakeSampleDiaryResponse)
//
//        // Activity 실행
//        scenario = ActivityScenario.launch(StyleExtractionActivity::class.java)
//    }
//
//    @After
//    fun tearDown() {
//        db.close() // 인메모리 DB 닫기
//        unmockkAll() // 모든 Mock 해제
//        scenario.close()
//    }
//
//    // --- 4. 테스트 케이스 ---
//
//    @Test
//    fun testExtractionSuccess_HappyPath() {
//        // Given: 유효한 데이터 (텍스트 3줄 + 이미지 2개 = 총 5개)
//        onView(withId(R.id.diaryTextInput)).perform(typeText("일기 1\n일기 2\n일기 3"))
//        stubImageSelectionResult(2) // 이미지 2개 선택 시뮬레이션
//        onView(withId(R.id.selectImagesButton)).perform(click())
//        onView(withId(R.id.selectedImageCount)).check(matches(withText("선택된 이미지: 2개")))
//
//        // When: '스타일 추출 실행' 버튼 클릭
//        onView(withId(R.id.runExtractionButton)).perform(click())
//
//        // Then: UI가 '분석 중' 상태로 변경됨
//        onView(withId(R.id.runExtractionButton)).check(matches(not(isEnabled())))
//        onView(withId(R.id.runExtractionButton)).check(matches(withText("스타일 분석 중...")))
//
//        // And: API들이 순서대로 호출됨
//        verify(exactly = 1) { mockApiService.extractStyle(any(), any()) }
//        coVerify(exactly = 1) { mockApiService.mergeMemos(any()) } // 샘플 일기 생성 API
//
//        // And: Activity가 성공적으로 종료됨
//        // (네트워크 응답이 비동기이므로 잠시 대기 후 상태 확인)
//        Thread.sleep(500) // 비동기 처리를 기다림 (더 나은 방법: Espresso IdlingResource)
//        assert(scenario.state == Lifecycle.State.DESTROYED)
//
//        // And: Room Database에 데이터가 저장됨 (가장 확실한 검증)
//        runBlocking {
//            // (파일 하단에 정의된 비공개 확장 함수 getOrAwaitValue 사용)
//            val styles = db.userStyleDao().getAllStyles().getOrAwaitValue()
//            assert(styles.isNotEmpty())
//            assert(styles[0].styleName == "나의 스타일 - 1번째")
//            assert(styles[0].sampleDiary == "생성된 샘플 일기입니다.")
//        }
//    }
//
//    @Test
//    fun testValidationFail_NotEnoughData() {
//        // Given: 불충분한 데이터 (텍스트 1줄 + 이미지 1개 = 총 2개)
//        onView(withId(R.id.diaryTextInput)).perform(typeText("일기 1"))
//        stubImageSelectionResult(1)
//        onView(withId(R.id.selectImagesButton)).perform(click())
//        onView(withId(R.id.selectedImageCount)).check(matches(withText("선택된 이미지: 1개")))
//
//        // When: '스타일 추출 실행' 버튼 클릭
//        onView(withId(R.id.runExtractionButton)).perform(click())
//
//        // Then: API가 호출되지 않음
//        verify(exactly = 0) { mockApiService.extractStyle(any(), any()) }
//        coVerify(exactly = 0) { mockApiService.mergeMemos(any()) }
//
//        // And: 버튼이 활성 상태로 유지됨
//        onView(withId(R.id.runExtractionButton)).check(matches(isEnabled()))
//        onView(withId(R.id.runExtractionButton)).check(matches(withText("스타일 추출 실행")))
//
//        // And: Activity가 종료되지 않음
//        assert(scenario.state == Lifecycle.State.RESUMED)
//    }
//
//    @Test
//    fun testExtractionFail_HttpError() {
//        // Given: API가 500 에러를 반환하도록 설정
//        val errorBody = "{\"message\":\"Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
//        stubExtractStyleResponse(Response.error(500, errorBody))
//
//        // And: 유효한 데이터 (텍스트 5줄)
//        onView(withId(R.id.diaryTextInput)).perform(typeText("1\n2\n3\n4\n5"))
//
//        // When: '스타일 추출 실행' 버튼 클릭
//        onView(withId(R.id.runExtractionButton)).perform(click())
//
//        // Then: UI가 다시 원래 상태로 리셋됨
//        onView(withId(R.id.runExtractionButton)).check(matches(isEnabled()))
//        onView(withId(R.id.runExtractionButton)).check(matches(withText("스타일 추출 실행")))
//
//        // And: 샘플 일기 생성 API는 호출되지 않음
//        coVerify(exactly = 0) { mockApiService.mergeMemos(any()) }
//
//        // And: Activity가 종료되지 않음
//        assert(scenario.state == Lifecycle.State.RESUMED)
//    }
//
//    @Test
//    fun testExtractionFail_NetworkFailure() {
//        // Given: API가 네트워크 실패(onFailure)를 반환하도록 설정
//        val mockCall: Call<StyleExtractionResponse> = mockk()
//        every { mockCall.enqueue(any()) } answers {
//            firstArg<Callback<StyleExtractionResponse>>().onFailure(mockCall, IOException("Network down"))
//        }
//        every { mockApiService.extractStyle(any(), any()) } returns mockCall
//
//        // And: 유효한 데이터 (텍스트 5줄)
//        onView(withId(R.id.diaryTextInput)).perform(typeText("1\n2\n3\n4\n5"))
//
//        // When: '스타일 추출 실행' 버튼 클릭
//        onView(withId(R.id.runExtractionButton)).perform(click())
//
//        // Then: UI가 다시 원래 상태로 리셋됨
//        onView(withId(R.id.runExtractionButton)).check(matches(isEnabled()))
//        onView(withId(R.id.runExtractionButton)).check(matches(withText("스타일 추출 실행")))
//
//        // And: 샘플 일기 생성 API는 호출되지 않음
//        coVerify(exactly = 0) { mockApiService.mergeMemos(any()) }
//    }
//
//    @Test
//    fun testBackButton_FinishesActivity() {
//        // When: 헤더의 뒤로가기 버튼 클릭 (R.id.header_back_icon은 include_settings_header.xml 내부 ID 가정)
//        onView(withId(R.id.header_back_icon)).perform(click())
//
//        // Then: Activity가 종료됨
//        assert(scenario.state == Lifecycle.State.DESTROYED)
//    }
//
//    // --- 5. 헬퍼 (Helper) ---
//
//    /**
//     * 이미지 선택 ActivityResult를 스터빙(Stubbing)합니다.
//     */
//    private fun stubImageSelectionResult(imageCount: Int) {
//        val imageUris = (1..imageCount).map {
//            Uri.parse("content://fake/image/$it")
//        }
//
//        val resultData = Intent().apply {
//            // GetMultipleContents는 ClipData를 사용
//            clipData = android.content.ClipData.newPlainText("uris", "")
//            imageUris.forEach { uri ->
//                clipData!!.addItem(android.content.ClipData.Item(uri))
//            }
//        }
//        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
//
//        // "image/*" 타입의 ACTION_GET_CONTENT 인텐트가 발생하면 'result'를 반환
//        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result)
//    }
//
//    /**
//     * extractStyle API의 응답을 설정하는 헬퍼 함수
//     */
//    private fun stubExtractStyleResponse(response: Response<StyleExtractionResponse>) {
//        val mockCall: Call<StyleExtractionResponse> = mockk()
//        every { mockCall.enqueue(any()) } answers {
//            firstArg<Callback<StyleExtractionResponse>>().onResponse(mockCall, response)
//        }
//        every { mockApiService.extractStyle(any(), any()) } returns mockCall
//    }
//
//
//    // --- 6. 이 파일에 포함된 유틸리티 ---
//
//    /**
//     * [HELPER CLASS] Coroutine Dispatcher 제어를 위한 JUnit Rule.
//     * 이 클래스 내부에서만 사용됩니다.
//     */
//    @ExperimentalCoroutinesApi
//    class MainCoroutineRule(
//        val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
//    ) : TestWatcher() {
//
//        override fun starting(description: Description) {
//            super.starting(description)
//            Dispatchers.setMain(testDispatcher)
//        }
//
//        override fun finished(description: Description) {
//            super.finished(description)
//            Dispatchers.resetMain()
//        }
//    }
//
//    /**
//     * [HELPER FUNCTION] LiveData의 값을 동기적으로 가져옵니다.
//     * 이 클래스 내부에서만 사용됩니다.
//     */
//    private fun <T> LiveData<T>.getOrAwaitValue(
//        time: Long = 2,
//        timeUnit: TimeUnit = TimeUnit.SECONDS
//    ): T {
//        var data: T? = null
//        val latch = CountDownLatch(1)
//        val observer = object : Observer<T> {
//            override fun onChanged(o: T?) {
//                data = o
//                latch.countDown()
//                this@getOrAwaitValue.removeObserver(this)
//            }
//        }
//
//        this.observeForever(observer)
//
//        try {
//            // 지정된 시간 동안 대기
//            if (!latch.await(time, timeUnit)) {
//                throw TimeoutException("LiveData value was never set.")
//            }
//        } finally {
//            this.removeObserver(observer)
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        return data as T
//    }
}