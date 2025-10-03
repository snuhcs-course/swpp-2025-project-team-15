plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.sumdays"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.first"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.security:security-crypto:1.0.0")
    // Retrofit: 네트워크 통신 라이브러리
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson Converter: JSON을 Kotlin 데이터 클래스로 변환
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp Logging Interceptor (선택사항): 통신 로그를 확인하여 디버깅에 유용
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    // SDK 26 미만에서 java time과 같은 날짜 및 시간 클래스를 사용할 수 있도록 해줌
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
}