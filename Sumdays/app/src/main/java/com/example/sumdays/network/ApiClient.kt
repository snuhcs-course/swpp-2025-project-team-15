package com.example.sumdays.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://172.30.1.67:3000/"  // Flask 서버 주소
    //폰에서 실행하려면 서버를 돌리는 컴퓨터의 IP를 여기 적어야 함

    // OkHttp 로깅 + 타임아웃 설정
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC   // 처음엔 BASIC으로 (BODY는 무거움)
    }

    private val okHttp by lazy {
        OkHttpClient.Builder()
            .connectTimeout(600, TimeUnit.SECONDS)   // 연결 타임아웃
            .readTimeout(600, TimeUnit.SECONDS)      // 응답 대기 시간
            .writeTimeout(600, TimeUnit.SECONDS)     // 요청 전송 시간
            .addInterceptor(logging)
            .build()
    }

    // Retrofit 인스턴스
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttp)   // 위에서 만든 okHttp 사용
            .build()
    }

    // API 인터페이스 구현체
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}