package com.example.sumdays.network

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import retrofit2.Retrofit

class RetrofitClientTest {

    @Test
    fun authApiService_isNotNull_andIsSingleton() {
        val instance1 = RetrofitClient.authApiService
        val instance2 = RetrofitClient.authApiService

        assertThat(instance1 === instance2, `is`(true))
    }

    @Test
    fun authApiService_isProxyImplementingAuthApiService() {
        val instance = RetrofitClient.authApiService
        val interfaces = instance.javaClass.interfaces.map { it.name }

        assertThat(interfaces.contains(AuthApiService::class.java.name), `is`(true))
    }
}
