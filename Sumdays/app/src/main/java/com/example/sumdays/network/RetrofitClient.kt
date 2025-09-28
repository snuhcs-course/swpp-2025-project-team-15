package com.example.sumdays.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // ★★★ 중요: Node.js 서버가 배포된 AWS 서버의 Public IP 주소 또는 도메인 주소를 입력하세요. ★★★
    // 예: "http://13.125.111.222:3000/" 또는 "https://api.sumdays.com/"
    private const val BASE_URL = "http://YOUR_AWS_SERVER_IP_OR_DOMAIN:3000/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

